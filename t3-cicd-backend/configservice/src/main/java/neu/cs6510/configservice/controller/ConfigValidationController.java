package neu.cs6510.configservice.controller;

import static neu.cs6510.shared.constants.RequestParameter.BRANCH;
import static neu.cs6510.shared.constants.RequestParameter.CONFIGPATH;
import static neu.cs6510.shared.constants.RequestParameter.PIPELINENAME;
import static neu.cs6510.shared.constants.RequestParameter.REPOURL;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import neu.cs6510.configservice.service.ConfigFileService;
import neu.cs6510.configservice.service.ValidationService;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.PipelineRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller class that handles the validation of GitLab CI/CD configuration files.
 * Provides an endpoint to upload and validate YAML configuration files.
 */
@Slf4j
@RestController
public class ConfigValidationController {

  @Autowired
  private PipelineRepository pipelineRepository;

  @Autowired
  private ValidationService validationService;

  @Autowired
  private ConfigFileService configFileService;

  /**
   * Endpoint to validate a YAML configuration file for a CI/CD pipeline.
   * This method processes the provided request parameters, clones the specified Git repository,
   * finds the YAML configuration file, and validates its contents.
   *
   * <p>Expected request parameters:
   * <ul>
   *   <li><strong>repo_url</strong>: URL of the Git repository to clone (required).</li>
   *   <li><strong>branch</strong>: Name of the branch to clone (required).</li>
   *   <li><strong>config_path</strong>: Relative path to the configuration file in the repository
   *       (optional, mutually exclusive with <code>pipeline_name</code>).</li>
   *   <li><strong>pipeline_name</strong>: Name of the pipeline to search for in the YAML
   *   configuration (optional, mutually exclusive with <code>config_path</code>).</li>
   * </ul>
   *
   * <p>Behavior:
   * <ul>
   *   <li>Validates the request parameters to ensure required fields are present.</li>
   *   <li>Clones the specified Git repository and branch to a persistent volume.</li>
   *   <li>Finds the configuration file either by its path or by matching the pipeline name.</li>
   *   <li>Parses and validates the configuration file, ensuring it meets the required
   *   specifications.</li>
   * </ul>
   *
   * @param requestParams A map containing the request parameters for validation:
   *                      - {@code repo_url}: the Git repository URL (required).
   *                      - {@code branch}: the branch name to clone (required).
   *                      - {@code config_path}: relative path to the configuration file (optional).
   *                      - {@code pipeline_name}: name of the pipeline in the YAML (optional).
   * @return {@code ResponseEntity<String>}:
   *         <ul>
   *           <li>{@code 200 OK}: If the validation is successful.</li>
   *           <li>{@code 400 Bad Request}: If an error occurs during validation, including:
   *             <ul>
   *               <li>Invalid or missing request parameters.</li>
   *               <li>Repository cloning failure.</li>
   *               <li>Failure to locate the configuration file.</li>
   *               <li>Validation errors in the configuration file.</li>
   *             </ul>
   *           </li>
   *         </ul>
   */
  @PostMapping("/validate")
  public ResponseEntity<Map<String, String>> validateYaml(
      @RequestBody Map<String, Object> requestParams) {
    log.info("Received /validate request: {}", requestParams);

    try {
      validateRequest(requestParams);
    } catch (IllegalArgumentException e) {
      log.error("Request validation failed: {}", e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("status", "error",
        "message", "Request parameter error: " + e.getMessage()));
    }

    String repoUrl = (String) requestParams.get(REPOURL);
    String branch = (String) requestParams.get(BRANCH);
    String configPath = (String) requestParams.get(CONFIGPATH);
    String pipelineName = (String) requestParams.get(PIPELINENAME);
    String repoDir;
    File configFile;

    try {
      repoDir = configFileService.cloneRepoToPv(repoUrl, branch);
    } catch (GitAPIException e) {
      log.error("Failed to clone repository: {}", e.getMessage(), e);
      return ResponseEntity.badRequest().body(Map.of("status", "error",
        "message", "Failed to clone repository: " + e.getMessage()));
    }

    try {
      configFile = configFileService.findConfigFile(repoDir, configPath, pipelineName);
      if (configFile == null) {
        log.warn("Configuration file not found for configPath: {} or pipelineName: {}",
            configPath, pipelineName);
        return ResponseEntity.badRequest().body(Map.of("status", "error",
          "message", "Config file not found"));
      }
    } catch (IOException e) {
      log.error("Error locating configuration file: {}", e.getMessage(), e);
      return ResponseEntity.badRequest().body(Map.of("status", "error",
        "message", "Failed to find configuration file: " + e.getMessage()));
    } catch (RuntimeException e) {
      log.error("Validation error while locating configuration file: {}", e.getMessage(), e);
      return ResponseEntity.badRequest().body(Map.of("status", "error",
        "message", "Validation error: " + e.getMessage()));
    }

    try {
      Pipeline pipeline = validationService.parseAndValidateConfigFile(configFile, repoUrl);
      Long id = pipeline.getId();
      pipeline.setRepoDir(repoDir);
      pipeline.setRepoUrl(repoUrl);
      pipeline.setConfigFilePath(configFile.getAbsolutePath());
      pipelineRepository.save(pipeline);
      log.info("Validation successful. Pipeline ID: {}", id);
      return ResponseEntity.ok(Map.of("status", "success", "pipelineId",
        id.toString(), "repoDir", repoDir, "configFilePath", configFile.getAbsolutePath()));
    } catch (IOException e) {
      log.error("Error validating configuration file: {}", e.getMessage(), e);
      return ResponseEntity.badRequest().body(Map.of("status", "error",
        "message", "Validation failed: " + e.getMessage()));
    } catch (RuntimeException e) {
      log.error("Validation error: {}", e.getMessage(), e);
      return ResponseEntity.badRequest().body(Map.of("status", "error",
        "message", "Validation error: " + e.getMessage()));
    }
  }

  /**
   * Validates the structure and content of the `pipeline/run` request API, ensuring it includes
   * essential parameters and only one of `config_path` or `pipeline_name`.
   * Throws an {@link IllegalArgumentException} if required fields are missing, empty, or invalid.
   *
   * @param requestParams the map of validation request parameters, which must include:
   *                      - `repo_url`: a non-empty string specifying the repository URL
   *                      - `branch`: a non-empty string specifying the branch name
   *                      - Either `config_path` or `pipeline_name` should be provided,
   *                        but not both.
   * @throws IllegalArgumentException if required fields are missing, empty, or if both
   *                                  `config_path` and `pipeline_name` are provided simultaneously
   *                                  or neither is provided.
   */
  static void validateRequest(Map<String, Object> requestParams) {
    log.debug("Validating request parameters: {}", requestParams);
    if (requestParams == null || requestParams.isEmpty()) {
      log.error("Request is empty.");
      throw new IllegalArgumentException("Request is empty");
    }

    if (!requestParams.containsKey(REPOURL)
        || !(requestParams.get(REPOURL) instanceof String)
        || ((String) requestParams.get(REPOURL)).isEmpty()) {
      log.error("Missing or invalid repo URL.");
      throw new IllegalArgumentException("Request must contain a non-empty string for repo URL");
    }
    if (!requestParams.containsKey(BRANCH)
        || !(requestParams.get(BRANCH) instanceof String)
        || ((String) requestParams.get(BRANCH)).isEmpty()) {
      log.error("Missing or invalid branch name.");
      throw new IllegalArgumentException("Request must contain a non-empty string for branch name");
    }

    // Validate that either config_path or pipeline_name is provided, but not both
    String configPath = (String) requestParams.get(CONFIGPATH);
    String pipelineName = (String) requestParams.get(PIPELINENAME);

    if (configPath == null && pipelineName == null) {
      log.error("Neither configPath nor pipelineName provided.");
      throw new IllegalArgumentException("One of configPath or pipelineName must be provided.");
    } else if (configPath != null && pipelineName != null) {
      log.error("Both configPath and pipelineName provided.");
      throw new IllegalArgumentException("Only one of configPath or pipelineName should be "
        + "provided, not both.");
    }
    log.debug("Request parameters validation completed successfully.");
  }
}
