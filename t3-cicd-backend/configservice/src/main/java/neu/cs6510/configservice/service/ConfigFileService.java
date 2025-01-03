package neu.cs6510.configservice.service;

import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_CONFIG_DIRECTORY;
import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_KEY_NAME;
import static neu.cs6510.shared.constants.ConfigFile.NON_JOB_KEY_DEFAULT;
import static neu.cs6510.shared.constants.ConfigFile.YAML_ALT_EXTENSION;
import static neu.cs6510.shared.constants.ConfigFile.YAML_EXTENSION;
import static neu.cs6510.shared.constants.Kubernetes.PV_PATH;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

/**
 * Service class responsible for managing configuration files for a CI/CD pipeline.
 * This includes cloning Git repositories, locating YAML configuration files, and
 * validating pipeline names within the configuration files.
 */
@Slf4j
@Service
public class ConfigFileService {

  /**
   * Clones a Git repository to a persistent volume.
   *
   * @param repoUrl the URL of the Git repository to clone.
   * @param branch  the branch to clone from the repository.
   * @return the local directory path where the repository was cloned.
   * @throws GitAPIException if an error occurs during the cloning process.
   */
  public String cloneRepoToPv(String repoUrl, String branch) throws GitAPIException {
    String repoDir = PV_PATH + repoUrl.substring(repoUrl.lastIndexOf('/') + 1)
        .replace(".git", "") + UUID.randomUUID();

    log.info("Cloning repository from URL: {} to branch: {} into directory: {}",
        repoUrl, branch, repoDir);
    try {
      Git.cloneRepository()
        .setURI(repoUrl)
        .setBranch(branch)
        .setDirectory(new File(repoDir))
          .call();
      log.info("Repository cloned successfully to {}", repoDir);
    } catch (GitAPIException e) {
      log.error("Failed to clone repository from URL: {}, branch: {}", repoUrl, branch, e);
      throw e;
    }

    return repoDir;
  }

  /**
   * Finds the configuration file in a cloned Git repository.
   * The file can be located either by a specific path or by searching for a pipeline name.
   *
   * @param repoDir      the directory of the cloned repository.
   * @param configPath   the relative path to the configuration file (if specified).
   * @param pipelineName the name of the pipeline to search for in the YAML files (if specified).
   * @return the configuration file if found, or {@code null} if no matching file is found.
   * @throws IOException if an error occurs while reading the files.
   * @throws RuntimeException if multiple YAML files with the specified pipeline name are found.
   */
  public File findConfigFile(String repoDir, String configPath, String pipelineName)
      throws IOException {
    log.info("Searching for configuration file in directory: {}, configPath: {}, "
        + "pipelineName: {}", repoDir, configPath, pipelineName);

    if (configPath != null && !configPath.isEmpty()) {
      File configFile = new File(repoDir, configPath);
      if (configFile.exists() && configFile.isFile()) {
        log.info("Configuration file found at specified path: {}", configFile.getAbsolutePath());
        return configFile;
      } else {
        log.warn("Configuration file not found at specified path: {}", configPath);
        return null;
      }
    }

    File[] yamlFiles = new File(repoDir + "/" + DEFAULT_CONFIG_DIRECTORY).listFiles(
      (dir, name) -> name.endsWith(YAML_EXTENSION) || name.endsWith(YAML_ALT_EXTENSION));

    if (yamlFiles == null || yamlFiles.length == 0) {
      log.warn("No YAML files found in directory: {}", repoDir);
      return null;
    }

    File matchingFile = null;
    int matchCount = 0;

    for (File yamlFile : yamlFiles) {
      if (containsPipelineName(yamlFile, pipelineName)) {
        matchCount++;
        if (matchCount > 1) {
          String errorMessage = String.format("Multiple YAML files with pipeline name '%s' found.",
              pipelineName);
          log.error(errorMessage);
          throw new RuntimeException(errorMessage);
        }
        matchingFile = yamlFile;
      }
    }

    if (matchingFile != null) {
      log.info("Matching configuration file found: {}", matchingFile.getAbsolutePath());
    } else {
      log.warn("No YAML files with pipeline name '{}' found in repository.", pipelineName);
    }

    return matchingFile;
  }

  /**
   * Checks if a given YAML file contains the specified pipeline name.
   *
   * @param yamlFile     the YAML file to search.
   * @param pipelineName the name of the pipeline to look for.
   * @return {@code true} if the pipeline name is found in the file, {@code false} otherwise.
   * @throws IOException if an error occurs while reading the YAML file.
   */
  private boolean containsPipelineName(File yamlFile, String pipelineName) throws IOException {
    log.debug("Checking if YAML file '{}' contains pipeline name '{}'",
        yamlFile.getAbsolutePath(), pipelineName);

    Yaml yaml = new Yaml();
    try (FileInputStream fis = new FileInputStream(yamlFile)) {
      Map<String, Object> yamlContent = yaml.load(fis);

      if (yamlContent.containsKey(NON_JOB_KEY_DEFAULT)) {
        Map<String, Object> defaultSection =
            (Map<String, Object>) yamlContent.get(NON_JOB_KEY_DEFAULT);

        if (defaultSection.containsKey(DEFAULT_KEY_NAME)
            && pipelineName.equals(defaultSection.get(DEFAULT_KEY_NAME))) {
          log.debug("Pipeline name '{}' found in YAML file '{}'",
              pipelineName, yamlFile.getAbsolutePath());
          return true;
        }
      }
    } catch (IOException e) {
      log.error("Error reading YAML file '{}': {}", yamlFile.getAbsolutePath(), e.getMessage(), e);
      throw e;
    }

    log.debug("Pipeline name '{}' not found in YAML file '{}'",
        pipelineName, yamlFile.getAbsolutePath());
    return false;
  }
}
