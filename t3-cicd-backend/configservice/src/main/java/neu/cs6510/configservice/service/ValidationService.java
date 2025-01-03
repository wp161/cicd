package neu.cs6510.configservice.service;

import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_KEY_DOCKER;
import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_KEY_DOCKER_IMAGE;
import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_KEY_DOCKER_REGISTRY;
import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_KEY_NAME;
import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_KEY_PATHS;
import static neu.cs6510.shared.constants.ConfigFile.DEFAULT_STAGES;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_ALLOW_FAILURE;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_ARTIFACTS;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_DOCKER;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_DOCKER_IMAGE;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_DOCKER_REGISTRY;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_NEEDS;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_PATHS;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_SCRIPT;
import static neu.cs6510.shared.constants.ConfigFile.JOB_KEY_STAGE;
import static neu.cs6510.shared.constants.ConfigFile.NON_JOB_KEYS;
import static neu.cs6510.shared.constants.ConfigFile.NON_JOB_KEY_DEFAULT;
import static neu.cs6510.shared.constants.ConfigFile.NON_JOB_KEY_STAGES;
import static neu.cs6510.shared.constants.ConfigFile.STAGES_KEY;
import static neu.cs6510.shared.constants.Docker.DOCKERHUB_REGISTRY;

import jakarta.transaction.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import neu.cs6510.configservice.utils.DuplicateKeyYamlConstructor;
import neu.cs6510.shared.entity.Job;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.entity.Stage;
import neu.cs6510.shared.repository.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Service class responsible for handling and parsing GitLab CI/CD YAML configuration files.
 */
@Service
@Slf4j
public class ValidationService {

  @Autowired
  private PipelineRepository pipelineRepository;

  private DumperOptions dumperOptions = new DumperOptions();
  private DuplicateKeyYamlConstructor duplicateKeyYamlConstructor =
      new DuplicateKeyYamlConstructor();

  private final Yaml yamlParser = new Yaml(duplicateKeyYamlConstructor,
      new Representer(dumperOptions), new DumperOptions(), new LoaderOptions());
  @Getter
  private Map<String, Stage> stageMap;
  @Getter
  private Map<String, Job> jobMap;
  @Getter
  private Pipeline pipeline;
  @Getter
  private String defaultRegistry;
  @Getter
  private String defaultImage;
  @Getter
  private List<String> defaultPaths;
  @Getter
  private Map<String, Object> config;
  @Getter
  private Map<String, Entry<Integer, Integer>> locations;
  @Getter
  private String fileName;

  /**
   * Parses the provided YAML configuration file and processes jobs and stages.
   *
   * @param file the uploaded multipart file containing the YAML configuration
   * @param repoUrl the URL of the repo
   * @return the ordered pipeline
   * @throws IOException if an error occurs while reading the file
   */
  @Transactional
  public Pipeline parseAndValidateConfigFile(File file, String repoUrl) throws IOException {
    log.info("Starting validation of the configuration file: {}", file.getName());
    fileName = file.getName();
    duplicateKeyYamlConstructor.setFileName(fileName);
    pipeline = new Pipeline();
    stageMap = new LinkedHashMap<>();
    jobMap = new HashMap<>();
    config = loadYaml(file);
    log.debug("Configuration file loaded: {}", fileName);
    defaultImage = null;
    defaultRegistry = null;
    locations = parseYamlWithLocations(file);
    log.debug("Parsed YAML file locations for: {}", fileName);
    handleConfigFile(config);
    log.info("Configuration file structure handled successfully for: {}", fileName);
    validateStages(1, 1);
    log.info("Stages validated successfully for pipeline: {}", pipeline.getName());
    processJobDependencies();
    log.info("Job dependencies processed and ordered for pipeline: {}", pipeline.getName());
    for (Stage stage : stageMap.values()) {
      pipeline.addStage(stage);  // Maintain stage order in the pipeline
      stage.setPipeline(pipeline);
    }
    pipeline.setRepoUrl(repoUrl);
    pipelineRepository.save(pipeline);
    log.info("Configuration file validated and pipeline saved successfully with {} "
        + "stages for repo URL: {}", stageMap.size(), repoUrl);
    return pipeline;
  }

  /**
   * Validates that there are no empty stages in the pipeline (i.e., stages with no jobs assigned).
   *
   * @param currentLine the Integer representing the line number
   * @param currentColumn the Integer representing the column number
   * @throws RuntimeException if any stage has no jobs
   */
  private void validateStages(Integer currentLine, Integer currentColumn) {
    log.debug("Validating stages for the pipeline...");
    for (Stage stage : stageMap.values()) {
      if (stage.getJobs().isEmpty()) {
        log.error("Stage '{}' has no jobs. Validation failed.", stage.getName());
        throw new RuntimeException(
          String.format("%s:%d:%d: Stage %s has no jobs.",
            fileName, currentLine, currentColumn, stage.getName())
        );
      }
    }
    log.debug("All stages validated successfully.");
  }

  /**
   * Orders jobs within each stage using topological sorting based on their dependencies and checks
   * for circular dependencies.
   *
   * @throws RuntimeException if any circular dependency is detected between jobs
   */
  private void processJobDependencies() {
    log.debug("Processing job dependencies for topological sorting...");
    for (Stage stage : stageMap.values()) {
      List<Job> jobs = stage.getJobs();
      if (jobs != null) {
        log.debug("Ordering jobs for stage: {}", stage.getName());
        topologicalSort(jobs);
      }
    }
    log.debug("All job dependencies processed successfully.");
  }


  /**
   * Loads and parses the YAML content from the provided file.
   *
   * @param file the file containing the YAML content
   * @return a map containing the parsed YAML content
   * @throws IOException if an error occurs while reading the file
   */
  private Map<String, Object> loadYaml(File file) throws IOException {
    log.debug("Loading YAML content from file: {}", file.getName());
    try (InputStream inputStream = new FileInputStream(file)) {
      return yamlParser.load(inputStream);
    }
  }

  /**
   * Method keeps track of the location of each word in the yaml file with each sublevel indicated
   * with a '.'.
   *
   * @param file the file containing the YAML content
   * @return a map containing the path and the line and column number of the path
   * @throws IOException if an error occurs while reading the file
   */
  private Map<String, Entry<Integer, Integer>> parseYamlWithLocations(File file)
      throws IOException {
    log.debug("Parsing YAML for line and column locations: {}", file.getName());
    try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file),
        StandardCharsets.UTF_8)) {
      Node root = yamlParser.compose(reader);
      Map<String, Entry<Integer, Integer>> locations = new HashMap<>();
      extractLocations(root, locations, "");
      log.debug("Locations parsed successfully for file: {}", file.getName());
      return locations;
    }
  }

  /**
   * Recursive helper method to iterate through mapping nodes to get the location of each word path.
   *
   * @param node the mapping node to iterate through
   * @param locations the map to store location information in for path : (line, column)
   * @param path the string path of the current word
   */
  private void extractLocations(Node node,
      Map<String, Entry<Integer, Integer>> locations, String path) {
    if (node instanceof MappingNode) {
      for (NodeTuple tuple : ((MappingNode) node).getValue()) {
        Node keyNode = tuple.getKeyNode();
        Node valueNode = tuple.getValueNode();

        String key = ((ScalarNode) keyNode).getValue();
        String currentPath = path.isEmpty() ? key : path + "." + key;
        locations.put(currentPath, Map.entry(keyNode.getStartMark().getLine() + 1,
            keyNode.getStartMark().getColumn() + 1));

        extractLocations(valueNode, locations, currentPath);
      }
    } else if (node instanceof SequenceNode) {
      for (int i = 0; i < ((SequenceNode) node).getValue().size(); i++) {
        Node itemNode = ((SequenceNode) node).getValue().get(i);
        String currentPath = path + "." + i; // List index as part of the path
        locations.put(currentPath, Map.entry(itemNode.getStartMark().getLine() + 1,
            itemNode.getStartMark().getColumn() + 1));
        extractLocations(itemNode, locations, currentPath);
      }
    }
  }

  /**
   * Helper method to return the error location.
   *
   * @param key the string to search for
   * @return the line number and column number map entry
   */
  private Entry<Integer, Integer> getErrorLocation(String key) {
    return locations.get(key);
  }

  /**
   * Processes the parsed YAML configuration file. It separates job entries from non-job entries,
   * validates jobs, and processes stages.
   *
   * @param config the parsed YAML configuration map
   * @throws RuntimeException if any invalid configuration is detected
   */
  private void handleConfigFile(Map<String, Object> config) {
    log.debug("Handling configuration file content.");
    if (!config.containsKey(NON_JOB_KEY_DEFAULT)) {
      log.error("Default section is missing in configuration file: {}", fileName);
      Integer lineNumber = 1;
      Integer columnNumber = 1;
      throw new RuntimeException(
        String.format("%s:%d:%d: Default section is not found.", fileName, lineNumber, columnNumber)
      );
    }
    log.debug("Default section found in configuration file.");

    log.debug("Parsing stages from configuration file...");
    parseStages(config.get(STAGES_KEY));
    log.debug("Stages parsed successfully. Total stages: {}", stageMap.size());

    // 1st pass: handle non-job entry
    log.debug("Processing non-job entries in the configuration file.");
    for (Entry<String, Object> entry : config.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (key == null) {
        Integer lineNumber = getErrorLocation(key).getKey();
        Integer columnNumber = getErrorLocation(key).getValue();
        log.error("Found null key in configuration file at {}:{}.", lineNumber, columnNumber);
        throw new RuntimeException(
          String.format("%s:%d:%d: Null key in config file.", fileName, lineNumber, columnNumber)
        );
      }
      if (value == null) {
        Integer lineNumber = getErrorLocation(key).getKey();
        Integer columnNumber = getErrorLocation(key).getValue();
        log.error("Found null value for key '{}' in configuration file at {}:{}.",
            key, lineNumber, columnNumber);
        throw new RuntimeException(
          String.format("%s:%d:%d: Cannot find value in Config File for key '%s'",
            fileName, lineNumber, columnNumber, key)
        );
      }

      if (NON_JOB_KEYS.contains(key)) {
        if (key.equals(NON_JOB_KEY_STAGES)) {
          log.debug("Skipping processing of stages key: {}", key);
          continue;  // stage has been processed
        }
        log.debug("Processing non-job entry: {}", key);
        handleNonJobEntry(key, value);
      }
    }
    log.debug("Non-job entries processed successfully.");

    // 2nd pass: handle job entry
    log.debug("Processing job entries in the configuration file.");
    for (Entry<String, Object> entry : config.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (!NON_JOB_KEYS.contains(key)) {
        Integer lineNumber = getErrorLocation(key).getKey();
        Integer columnNumber = getErrorLocation(key).getValue();
        log.debug("Validating and processing job entry: {}", key);
        checkValueType(value, Map.class, key, fileName, lineNumber, columnNumber);
        handleJobEntry(key, (Map<String, Object>) value);
      }
    }
    log.debug("Configuration file processed successfully.");
  }

  /**
   * Handles the processing of a job entry from the YAML configuration. It validates that the job
   * contains the necessary fields such as a script, stage, and Docker settings if necessary. It
   * also sets default Docker values if they aren't provided.
   *
   * @param jobName   the name of the job
   * @param jobConfig the configuration map for the job
   * @throws RuntimeException if the job configuration is invalid
   */
  private void handleJobEntry(String jobName, Map<String, Object> jobConfig) {
    log.debug("Validating job: {}", jobName);
    Integer lineNumber = getErrorLocation(jobName).getKey();
    Integer columnNumber = getErrorLocation(jobName).getValue();
    // check if job has script
    if (!jobConfig.containsKey(JOB_KEY_SCRIPT)) {
      log.error("Job '{}' has no script. Validation failed.", jobName);
      throw new RuntimeException(
        String.format("%s:%d:%d: Job %s has no script.",
          fileName, lineNumber, columnNumber, jobName)
      );
    }

    // check if job declares a stage
    if (!jobConfig.containsKey(JOB_KEY_STAGE)) {
      log.error("Job '{}' has no stage. Validation failed.", jobName);
      throw new RuntimeException(
        String.format("%s:%d:%d: Job %s has no stage.",
          fileName, lineNumber, columnNumber, jobName)
      );
    }

    // validate if job can inherit Docker image from global and use default registry
    if (!jobConfig.containsKey(JOB_KEY_DOCKER)) {
      log.debug("No Docker configuration provided for job '{}'. Using default values.",
          jobName);
      if (defaultImage == null) {
        log.error("Validation failed: No Docker image provided or inherited for job '{}'.",
            jobName);
        throw new RuntimeException(
          String.format("%s:%d:%d: Docker image not found for Job %s.",
            fileName, lineNumber, columnNumber, jobName)
        );
      }

      if (defaultRegistry == null) {
        defaultRegistry = DOCKERHUB_REGISTRY;
      }
    }

    List<String> jobScripts = new ArrayList<>();
    Stage jobStage = null;
    String registry = "";
    String imageName = "";
    Boolean allowFailure = false;
    List<String> jobNeeds = List.of();
    List<String> paths = List.of();
    if (defaultPaths != null) {
      paths = defaultPaths;
    }

    log.debug("Processing job configuration for '{}'.", jobName);
    for (Entry<String, Object> entry : jobConfig.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key.equals(JOB_KEY_STAGE)) {
        String stage = handleJobStage(jobName, value);
        jobStage = stageMap.get(stage);
        log.debug("Assigned job '{}' to stage '{}'.", jobName, stage);
      }

      if (key.equals(JOB_KEY_SCRIPT)) {
        lineNumber = getErrorLocation(jobName + "." + JOB_KEY_SCRIPT).getKey();
        columnNumber = getErrorLocation(jobName + "." + JOB_KEY_SCRIPT).getValue();
        checkValueType(value, List.class, JOB_KEY_SCRIPT, fileName, lineNumber, columnNumber);
        checkListType(
            (List<?>) value, String.class, JOB_KEY_SCRIPT, fileName, lineNumber, columnNumber);
        jobScripts.addAll((List<String>) value);
        log.debug("Added scripts for job '{}': {}", jobName, jobScripts);
      }

      if (key.equals(JOB_KEY_NEEDS)) {
        lineNumber = getErrorLocation(jobName + "." + JOB_KEY_NEEDS).getKey();
        columnNumber = getErrorLocation(jobName + "." + JOB_KEY_NEEDS).getValue();
        checkValueType(value, List.class, key, fileName, lineNumber, columnNumber);
        checkListType((List<?>) value, String.class, key, fileName, lineNumber, columnNumber);
        jobNeeds = (List<String>) value;
        log.debug("Job '{}' has dependencies: {}", jobName, jobNeeds);
      }

      if (key.equals(JOB_KEY_DOCKER)) {
        Map<String, String> docker = handleDocker(jobName, value);
        if (docker.containsKey(JOB_KEY_DOCKER_REGISTRY)) {
          registry = docker.get(JOB_KEY_DOCKER_REGISTRY);
        }
        if (docker.containsKey(JOB_KEY_DOCKER_IMAGE)) {
          imageName = docker.get(JOB_KEY_DOCKER_IMAGE);
        }
        log.debug("Docker configuration for job '{}': registry='{}', image='{}'.",
            jobName, registry, imageName);
      }

      if (key.equals(JOB_KEY_ALLOW_FAILURE)) {
        lineNumber = getErrorLocation(jobName + "." + JOB_KEY_ALLOW_FAILURE).getKey();
        columnNumber = getErrorLocation(jobName + "." + JOB_KEY_ALLOW_FAILURE).getValue();
        checkValueType(value, Boolean.class, key, fileName, lineNumber, columnNumber);
        allowFailure = (Boolean) value;
        log.debug("Job '{}' is allowed to fail: {}", jobName, allowFailure);
      }

      if (key.equals(JOB_KEY_ARTIFACTS)) {
        List<String> jobPaths = handleArtifact(jobName, value);
        if (jobPaths != null && !jobPaths.isEmpty()) {
          paths = jobPaths;
          log.debug("Artifacts paths for job '{}': {}", jobName, paths);
        }
      }
    }

    if (registry.equals("")) {
      registry = DOCKERHUB_REGISTRY;
    }
    if (imageName.equals("")) {
      if (defaultImage == null) {
        log.error("No default image found for job '{}'", jobName);
        throw new RuntimeException(
          String.format("%s:%d:%d: Docker image not found for Job %s.",
            fileName, lineNumber, columnNumber, jobName)
        );
      } else {
        imageName = defaultImage;
      }
    }

    Job job = Job.builder()
        .name(jobName)
        .stage(jobStage)
        .scripts(jobScripts)
        .needs(jobNeeds)
        .registry(registry)
        .imageName(imageName)
        .paths(paths)
        .allowFailure(allowFailure)
        .build();
    log.debug("Job built successfully: {}", job.toString());
    stageMap.get(job.getStage().getName()).addJob(job);
    jobMap.put(jobName, job);
    log.info("Job '{}' successfully validated and added to stage '{}'.",
        jobName, jobStage.getName());
  }

  /**
   * Handles the artifacts configuration for a job, validating and extracting the artifact paths.
   * The method ensures that the provided artifacts configuration is of the correct type and
   * extracts the paths associated with the artifacts.
   *
   * @param jobName the name of the current job
   * @param value the artifacts configuration object, which is expected to be a Map containing
   *              the paths for the artifacts.
   * @return a List of artifact paths. If the paths are not defined, an empty list is returned.
   * @throws RuntimeException if the value is not a valid artifacts configuration or
   *         contains incorrect data types for the paths.
   */
  private List<String> handleArtifact(String jobName, Object value) {
    log.debug("Processing artifacts configuration for job: {}", jobName);
    List<String> paths = new ArrayList<>();
    Integer lineNumber = getErrorLocation(jobName + "." + JOB_KEY_ARTIFACTS).getKey();
    Integer columnNumber = getErrorLocation(jobName + "." + JOB_KEY_ARTIFACTS).getValue();
    checkValueType(value, Map.class, JOB_KEY_ARTIFACTS, fileName, lineNumber, columnNumber);
    log.debug("Artifacts configuration is a valid Map for job: {}", jobName);
    Map<String, Object> artifacts = (Map<String, Object>) value;
    if (artifacts.containsKey(JOB_KEY_PATHS)) {
      log.debug("Found 'paths' key in artifacts configuration for job: {}", jobName);
      lineNumber = getErrorLocation(
        jobName + "." + JOB_KEY_ARTIFACTS + "." + JOB_KEY_PATHS).getKey();
      columnNumber = getErrorLocation(
        jobName + "." + JOB_KEY_ARTIFACTS + "." + JOB_KEY_PATHS).getValue();
      checkValueType(
          artifacts.get(JOB_KEY_PATHS), List.class, JOB_KEY_PATHS, fileName,
          lineNumber, columnNumber);
      checkListType(
          (List<String>) artifacts.get(JOB_KEY_PATHS), String.class, JOB_KEY_PATHS, fileName,
          lineNumber, columnNumber);
      paths = (List<String>) artifacts.get(JOB_KEY_PATHS);
    }
    log.info("Extracted {} artifact paths for job: {}", paths.size(), jobName);
    return paths;
  }

  /**
   * Handles the Docker configuration for a job, validating and extracting the Docker registry
   * and image details. The method ensures the configuration is of the correct type and stores
   * the Docker registry and image in a map.
   *
   * @param jobName the name of the current job
   * @param value the Docker configuration object which is expected to be a Map
   * @return a Map containing the Docker registry and image. The map keys are
   *         {@code JOB_KEY_DOCKER_REGISTRY} and {@code JOB_KEY_DOCKER_IMAGE}.
   * @throws RuntimeException if the value is not a valid Docker configuration or
   *         contains incorrect data types.
   */
  private Map<String, String> handleDocker(String jobName, Object value) {
    log.debug("Handling Docker configuration for job: {}", jobName);
    Map<String, String> docker = new HashMap<>();
    Integer lineNumber = getErrorLocation(jobName + "." + JOB_KEY_DOCKER).getKey();
    Integer columnNumber = getErrorLocation(jobName + "." + JOB_KEY_DOCKER).getValue();
    checkValueType(value, Map.class, JOB_KEY_DOCKER, fileName, lineNumber, columnNumber);
    Map<String, Object> dockerConfig = (Map<String, Object>) value;
    if (dockerConfig.containsKey(DEFAULT_KEY_DOCKER_REGISTRY)) {
      log.debug("Found Docker registry for job: {}", jobName);
      lineNumber = getErrorLocation(
        jobName + "." + JOB_KEY_DOCKER + "." + JOB_KEY_DOCKER_REGISTRY).getKey();
      columnNumber = getErrorLocation(
        jobName + "." + JOB_KEY_DOCKER + "." + JOB_KEY_DOCKER_REGISTRY).getValue();
      checkValueType(dockerConfig.get(JOB_KEY_DOCKER_REGISTRY), String.class,
          JOB_KEY_DOCKER_REGISTRY, fileName, lineNumber, columnNumber);
      docker.put(JOB_KEY_DOCKER_REGISTRY, dockerConfig.get(JOB_KEY_DOCKER_REGISTRY).toString());
    }
    if (dockerConfig.containsKey(JOB_KEY_DOCKER_IMAGE)) {
      log.debug("Found Docker image for job: {}", jobName);
      lineNumber = getErrorLocation(
        jobName + "." + JOB_KEY_DOCKER + "." + JOB_KEY_DOCKER_IMAGE).getKey();
      columnNumber = getErrorLocation(
        jobName + "." + JOB_KEY_DOCKER + "." + JOB_KEY_DOCKER_IMAGE).getValue();
      checkValueType(
          dockerConfig.get(JOB_KEY_DOCKER_IMAGE), String.class, JOB_KEY_DOCKER_IMAGE,
          fileName, lineNumber, columnNumber);
      docker.put(JOB_KEY_DOCKER_IMAGE, dockerConfig.get(JOB_KEY_DOCKER_IMAGE).toString());
    }
    log.debug("Docker configuration handled for job: {}", jobName);
    return docker;
  }

  /**
   * Parses the stages defined in the configuration, ensuring that stage names are unique. If no
   * user-defined stages are provided, default stages are used.
   *
   * @param value the stages section from the YAML configuration
   * @throws RuntimeException if the stage names are not unique or invalid types are encountered
   */
  private void parseStages(Object value) {
    log.debug("Parsing stages from configuration...");
    // If no user-defined stages, use default stages
    if (value == null || ((List<?>) value).isEmpty()) {
      log.debug("No user-defined stages found. Using default stages.");
      for (String defaultStage : DEFAULT_STAGES) {
        Stage stage = Stage.builder().name(defaultStage).build();
        stageMap.put(stage.getName(), stage);
      }
    } else {
      Integer lineNumber = getErrorLocation(STAGES_KEY).getKey();
      Integer columnNumber = getErrorLocation(STAGES_KEY).getValue();
      checkValueType(value, List.class, STAGES_KEY, fileName, lineNumber, columnNumber);
      List<?> stageNames = (List<?>) value;
      checkListType(stageNames, String.class, STAGES_KEY, fileName, lineNumber, columnNumber);
      log.debug("User-defined stages found. Validating uniqueness and adding to stage map.");
      int index = 0;
      for (Object stageNameObj : stageNames) {
        String stageName = (String) stageNameObj;
        // stage names must be unique
        if (stageMap.containsKey(stageName)) {
          log.error("Duplicate stage name detected: {}", stageName);
          lineNumber = getErrorLocation(stageName).getKey();
          columnNumber = getErrorLocation(stageName).getValue();
          throw new RuntimeException(
            String.format("%s:%d:%d: Duplicate stage name %s",
              fileName, lineNumber, columnNumber, stageName)
          );
        }
        Stage stage = Stage.builder().name(stageName).build();
        stageMap.put(stage.getName(), stage);
        index += 1;
      }
    }
    log.debug("Stages parsed successfully. Total stages: {}", stageMap.size());
  }

  /**
   * Handles the stage configuration for the job, ensuring the stage is declared.
   *
   * @param jobName the name of the current job
   * @param value the stage value from the configuration
   * @return the parsed stage name
   * @throws RuntimeException if the stage is not declared or invalid types are encountered
   */
  private String handleJobStage(String jobName, Object value) {
    log.debug("Processing stage for job: {}", jobName);
    Integer lineNumber = getErrorLocation(jobName + "." + JOB_KEY_STAGE).getKey();
    Integer columnNumber = getErrorLocation(jobName + "." + JOB_KEY_STAGE).getValue();
    checkValueType(value, String.class, JOB_KEY_STAGE, fileName, lineNumber, columnNumber);
    String stage = (String) value;
    if (stage != null) {
      if (!stageMap.containsKey(stage)) {
        log.error("Stage '{}' is not declared. Validation failed for job: {}", stage, jobName);
        throw new RuntimeException(
          String.format("%s:%d:%d: Stage name %s is not declared.",
            fileName, lineNumber, columnNumber, stage)
        );
      }
      log.debug("Stage '{}' validated successfully for job: {}", stage, jobName);
      return stage;
    }
    log.warn("No stage declared for job: {}", jobName);
    return "";
  }

  /**
   * Handles non-job entries in the YAML configuration, such as global defaults, stages, or paths.
   *
   * @param key   the key of the non-job entry
   * @param value the value of the non-job entry
   * @throws RuntimeException if any invalid configuration is detected
   */
  private void handleNonJobEntry(String key, Object value) {
    log.debug("Processing non-job entry: {}", key);
    if (key.equals(NON_JOB_KEY_DEFAULT)) {
      log.debug("Handling 'default' section in configuration.");
      handleDefault(key, value);
      log.debug("'Default' section processed successfully.");
    } else {
      log.warn("Unknown non-job entry: {}. Skipping processing.", key);
    }
  }

  /**
   * Validates that the provided object is of the specified type. If the object is null or not of
   * the expected type, this method throws an {@link IllegalArgumentException}.
   *
   * @param object  The object to be validated.
   * @param clazz   The expected class type of the object.
   * @param keyName The key associated with the object in the configuration for error reporting.
   * @param fileName The name of the file being processed
   * @param lineNumber the line number in the yaml file we are at
   * @param columnNumber the column number in the yaml file we are at
   * @throws IllegalArgumentException if the object is null or not of the expected type.
   */
  public static void checkValueType(Object object, Class<?> clazz,
      String keyName, String fileName, Integer lineNumber, Integer columnNumber) {
    log.debug("Validating type for key '{}' at {}:{} in file '{}'. Expected type: {}",
        keyName, lineNumber, columnNumber, fileName, clazz.getSimpleName());
    if (object == null) {
      log.error("Validation failed: Value for key '{}' is null at {}:{} in file '{}'.",
          keyName, lineNumber, columnNumber, fileName);
      throw new RuntimeException(
        String.format("%s:%d:%d: The value in key %s is null, expected a %s.",
          fileName, lineNumber, columnNumber, keyName, clazz.getSimpleName())
      );
    }

    if (!clazz.isInstance(object)) {
      log.error("Validation failed: Wrong type for key '{}' at {}:{} in file '{}'. Expected: {}, "
          + "Found: {}", keyName, lineNumber, columnNumber, fileName,
          clazz.getSimpleName(), object.getClass().getSimpleName());
      throw new RuntimeException(
        String.format("%s:%d:%d: Wrong type for value in key %s. Expected a %s, "
            + "but found %s with value: %s.",
        fileName, lineNumber, columnNumber, keyName,
          clazz.getSimpleName(), object.getClass().getSimpleName(), object)
      );
    }
    log.debug("Validation succeeded for key '{}' at {}:{} in file '{}'.",
        keyName, lineNumber, columnNumber, fileName);
  }

  /**
   * Validates that the provided list is not empty and that all its elements are of the specified
   * type. If the list is null, empty, or contains elements of the wrong type, this method throws an
   * {@link IllegalArgumentException}.
   *
   * @param list         The list to be validated.
   * @param expectedType The expected class type of elements in the list.
   * @param key          The key associated with the list in the configuration for error reporting.
   * @param fileName The name of the file being processed
   * @param lineNumber the line number in the yaml file we are at
   * @param columnNumber the column number in the yaml file we are at
   * @throws IllegalArgumentException if the list is null, empty, or contains elements of the wrong
   *                                  type.
   */
  public static void checkListType(List<?> list, Class<?> expectedType,
      String key, String fileName, Integer lineNumber, Integer columnNumber) {
    log.debug("Validating list for key '{}' at {}:{} in file '{}'. Expected element type: {}",
        key, lineNumber, columnNumber, fileName, expectedType.getSimpleName());
    if (list == null) {
      log.error("Validation failed: List for key '{}' is null at {}:{} in file '{}'.",
          key, lineNumber, columnNumber, fileName);
      throw new IllegalArgumentException(
        String.format("%s:%d:%d: List for key %s is null.", fileName, lineNumber, columnNumber, key)
      );
    }
    if (list.isEmpty()) {
      log.error("Validation failed: List for key '{}' is empty at {}:{} in file '{}'.",
          key, lineNumber, columnNumber, fileName);
      throw new RuntimeException(
        String.format("%s:%d:%d: List for key %s is empty.",
          fileName, lineNumber, columnNumber, key)
      );
    }
    for (Object item : list) {
      if (!expectedType.isInstance(item)) {
        if (item != null) {
          log.error("Validation failed: Incorrect type in list for key '{}' at {}:{} in file '{}'. "
              + "Expected: {}, Found: {}", key, lineNumber, columnNumber, fileName,
              expectedType.getSimpleName(), item.getClass().getSimpleName());
          throw new RuntimeException(
            String.format("%s:%d:%d: List for key %s contains an item of incorrect type. "
                + "Expected %s, but found %s with value: %s",
            fileName, lineNumber, columnNumber, key,
              expectedType.getSimpleName(), item.getClass().getSimpleName(), item)
          );
        }
        log.error("Validation failed: Null item in list for key '{}' at {}:{} in file '{}'. "
            + "Expected type: {}", key, lineNumber, columnNumber, fileName,
            expectedType.getSimpleName());
        throw new RuntimeException(
          String.format("%s:%d:%d: List for key %s contains an item of incorrect type. "
              + "Expected %s, but found null item of null value",
          fileName, lineNumber, columnNumber, key, expectedType.getSimpleName())
        );
      }
    }
    log.debug("Validation succeeded for list in key '{}' at {}:{} in file '{}'.",
        key, lineNumber, columnNumber, fileName);
  }

  /**
   * Handles default values like pipeline name, Docker configurations, and paths.
   *
   * @param key the string that we want to look up the location of
   * @param value the default entry in the YAML configuration
   * @throws RuntimeException if any invalid or missing configurations are detected
   */
  private void handleDefault(String key, Object value) {
    log.debug("Processing 'default' section in the configuration file.");
    Integer lineNumber = getErrorLocation(key).getKey();
    Integer columnNumber = getErrorLocation(key).getValue();
    checkValueType(value, Map.class, NON_JOB_KEY_DEFAULT, fileName, lineNumber, columnNumber);
    Map<String, Object> globalConfig = (Map<String, Object>) value;

    // handle name
    if (!globalConfig.containsKey(DEFAULT_KEY_NAME)
        || globalConfig.get(DEFAULT_KEY_NAME) == null) {
      lineNumber = getErrorLocation(NON_JOB_KEY_DEFAULT).getKey();
      columnNumber = getErrorLocation(NON_JOB_KEY_DEFAULT).getValue();
      log.error("Pipeline name is missing in the 'default' section.");
      throw new RuntimeException(
            String.format("%s:%d:%d: Pipeline name is not defined.", fileName,
                lineNumber, columnNumber)
          );
    }
    lineNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "." + DEFAULT_KEY_NAME).getKey();
    columnNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "." + DEFAULT_KEY_NAME).getValue();
    checkValueType(globalConfig.get(DEFAULT_KEY_NAME), String.class, DEFAULT_KEY_NAME, fileName,
        lineNumber, columnNumber);
    String name = globalConfig.get(DEFAULT_KEY_NAME).toString();
    pipeline.setName(name);
    log.info("Pipeline name set to: {}", name);

    // handle docker
    if (globalConfig.containsKey(DEFAULT_KEY_DOCKER)) {
      log.debug("Processing default Docker configuration.");
      lineNumber = getErrorLocation(
        NON_JOB_KEY_DEFAULT + "." + DEFAULT_KEY_DOCKER).getKey();
      columnNumber = getErrorLocation(
        NON_JOB_KEY_DEFAULT + "." + DEFAULT_KEY_DOCKER).getValue();
      checkValueType(globalConfig.get(DEFAULT_KEY_DOCKER), Map.class, DEFAULT_KEY_DOCKER, fileName,
          lineNumber, columnNumber);
      Map<String, Object> dockerConfig = (Map<String, Object>) globalConfig.get(DEFAULT_KEY_DOCKER);
      if (dockerConfig.containsKey(DEFAULT_KEY_DOCKER_REGISTRY)
          && dockerConfig.get(DEFAULT_KEY_DOCKER_REGISTRY) != null
      ) {
        lineNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "."
          + DEFAULT_KEY_DOCKER + "." + DEFAULT_KEY_DOCKER_REGISTRY).getKey();
        columnNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "."
          + DEFAULT_KEY_DOCKER + "." + DEFAULT_KEY_DOCKER_REGISTRY).getValue();
        checkValueType(dockerConfig.get(DEFAULT_KEY_DOCKER_REGISTRY), String.class,
            DEFAULT_KEY_DOCKER_REGISTRY, fileName, lineNumber, columnNumber);
        log.info("Default Docker registry set to: {}", defaultRegistry);
        defaultRegistry = dockerConfig.get(DEFAULT_KEY_DOCKER_REGISTRY).toString();
      }
      if (dockerConfig.containsKey(DEFAULT_KEY_DOCKER_IMAGE)
          && dockerConfig.get(DEFAULT_KEY_DOCKER_IMAGE) != null
      ) {
        lineNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "."
          + DEFAULT_KEY_DOCKER + "." + DEFAULT_KEY_DOCKER_IMAGE).getKey();
        columnNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "."
          + DEFAULT_KEY_DOCKER + "." + DEFAULT_KEY_DOCKER_IMAGE).getValue();
        checkValueType(dockerConfig.get(DEFAULT_KEY_DOCKER_IMAGE), String.class,
            DEFAULT_KEY_DOCKER_IMAGE, fileName, lineNumber, columnNumber);
        defaultImage = dockerConfig.get(DEFAULT_KEY_DOCKER_IMAGE).toString();
        log.info("Default Docker image set to: {}", defaultImage);
      }
    }

    // handle paths
    if (globalConfig.containsKey(DEFAULT_KEY_PATHS)
        && globalConfig.get(DEFAULT_KEY_PATHS) != null
    ) {
      log.debug("Processing default artifact paths.");
      lineNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "."
        + DEFAULT_KEY_PATHS).getKey();
      columnNumber = getErrorLocation(NON_JOB_KEY_DEFAULT + "."
        + DEFAULT_KEY_PATHS).getValue();
      checkValueType(globalConfig.get(DEFAULT_KEY_PATHS), List.class,
          DEFAULT_KEY_PATHS, fileName, lineNumber, columnNumber);
      defaultPaths = (List<String>) globalConfig.get(DEFAULT_KEY_PATHS);
      log.info("Default artifact paths set to: {}", defaultPaths);
    }
    log.debug("Completed processing 'default' section.");
  }

  /**
   * Performs a topological sort of the jobs in a stage based on their dependencies.
   *
   * @param jobs the list of jobs in the stage
   */
  private void topologicalSort(List<Job> jobs) {
    log.debug("Starting topological sort for {} jobs in the stage.", jobs.size());
    Map<Job, Integer> inDegree = new HashMap<>();
    Map<Job, List<Job>> graph = new HashMap<>();

    // initialize the graph
    for (Job job : jobs) {
      inDegree.put(job, 0);
      graph.put(job, new ArrayList<>());
    }
    log.debug("Graph initialized with jobs: {}", jobs.stream().map(Job::getName).toList());

    // populate in-degree and connect nodes
    for (Job job : jobs) {
      if (job.getNeeds() != null) {
        for (String dependencyName : job.getNeeds()) {
          Job dependency = jobMap.get(dependencyName);
          if (dependency != null) {
            if (!dependency.getStage().equals(job.getStage())) {
              Integer lineNumber = getErrorLocation(job.getName() + "."
                  + JOB_KEY_NEEDS).getKey();
              Integer columnNumber = getErrorLocation(job.getName() + "."
                  + JOB_KEY_NEEDS).getValue();
              log.error("Dependency stage mismatch for job '{}' on dependency '{}'.",
                  job.getName(), dependencyName);
              throw new RuntimeException(
                String.format("%s:%d:%d: Job %s is defining a dependency %s that "
                    + "belongs to a different stage",
                  fileName, lineNumber, columnNumber, job.getName(), dependencyName)
              );
            }
            inDegree.put(job, inDegree.get(job) + 1);
            graph.get(dependency).add(job);
            log.debug("Dependency added: {} -> {}", dependencyName, job.getName());
          } else {
            Integer lineNumber = getErrorLocation(job.getName() + "."
                + JOB_KEY_NEEDS).getKey();
            Integer columnNumber = getErrorLocation(job.getName() + "."
                + JOB_KEY_NEEDS).getValue();
            log.error("Undefined dependency '{}' required by job '{}'.",
                dependencyName, job.getName());
            throw new RuntimeException(
              String.format("%s:%d:%d: Job %s is not defined but is needed by job %s.",
                fileName, lineNumber, columnNumber, dependencyName, job.getName())
            );
          }
        }
      }
    }
    log.debug("Graph and in-degree populated for all jobs.");

    // topological sort
    Queue<Job> queue = new LinkedList<>();
    List<List<Job>> orderedJobs = new ArrayList<>();

    for (Job job : jobs) {
      if (inDegree.get(job) == 0) {
        // add all jobs with no dependencies in the queue and start with them
        queue.add(job);
        log.debug("Job with no dependencies added to queue: {}", job.getName());
      }
    }

    while (!queue.isEmpty()) {
      int levelSize = queue.size(); // Number of jobs in the current level
      List<Job> currentLevelJobs = new ArrayList<>();
      for (int i = 0; i < levelSize; i++) {
        Job job = queue.poll();
        currentLevelJobs.add(job);
        for (Job dependentJob : graph.get(job)) {
          inDegree.put(dependentJob, inDegree.get(dependentJob) - 1);
          if (inDegree.get(dependentJob) == 0) {
            queue.add(dependentJob);
            log.debug("Job with dependencies resolved added to queue: {}", dependentJob.getName());
          }
        }
      }
      // Add all jobs of the current level to the result
      orderedJobs.add(currentLevelJobs);
      log.debug("Completed level with {} jobs: {}", currentLevelJobs.size(),
          currentLevelJobs.stream().map(Job::getName).toList());
    }

    if (orderedJobs.stream().mapToInt(List::size).sum() < jobs.size()) {
      List<Job> cyclicJobs = new ArrayList<>();
      for (Job job : jobs) {
        if (inDegree.get(job) > 0) {
          cyclicJobs.add(job);
        }
      }
      Integer lineNumber = getErrorLocation(cyclicJobs.get(0).getName()).getKey();
      Integer columnNumber = getErrorLocation(cyclicJobs.get(0).getName()).getValue();
      StringBuilder cycleMessage = new StringBuilder(fileName + ":"
          + lineNumber + ":" + columnNumber + ":"
          + " Circular dependency detected involving jobs: ");
      for (Job cyclicJob : cyclicJobs) {
        cycleMessage.append(cyclicJob.getName()).append(", ");
      }
      if (cycleMessage.length() > 0) {
        cycleMessage.setLength(cycleMessage.length() - 2);  // Remove the last ", "
      }
      cycleMessage.append(".");
      log.error("Circular dependency detected. Jobs involved: {}",
          cyclicJobs.stream().map(Job::getName).toList());
      throw new RuntimeException(cycleMessage.toString());

    }
    log.info("Topological sort completed successfully for {} jobs.", jobs.size());
  }
}
