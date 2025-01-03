package neu.cs6510.shared.constants;

import java.util.Set;

/**
 *  A class that holds various constant used mainly for Config File.
 */
public class ConfigFile {

  public static final String DEFAULT_KEY_NAME = "name";
  public static final String DEFAULT_KEY_DOCKER = "docker";
  public static final String DEFAULT_KEY_DOCKER_REGISTRY = "registry";
  public static final String DEFAULT_KEY_DOCKER_IMAGE = "image";
  public static final String DEFAULT_KEY_PATHS = "paths";

  public static final String STAGES_KEY = "stages";
  public static final String DEFAULT_STAGE_BUILD = "build";
  public static final String DEFAULT_STAGE_TEST = "test";
  public static final String DEFAULT_STAGE_DOC = "doc";
  public static final String DEFAULT_STAGE_DEPLOY = "deploy";
  public static final Set<String> DEFAULT_STAGES = Set.of(
      DEFAULT_STAGE_BUILD,
      DEFAULT_STAGE_TEST,
      DEFAULT_STAGE_DOC,
      DEFAULT_STAGE_DEPLOY);

  public static final String NON_JOB_KEY_DEFAULT = "default";
  public static final String NON_JOB_KEY_INCLUDE = "include";
  public static final String NON_JOB_KEY_STAGES = "stages";
  public static final String NON_JOB_KEY_WORKFLOW = "workflow";

  public static final Set<String> NON_JOB_KEYS = Set.of(
      NON_JOB_KEY_DEFAULT,
      NON_JOB_KEY_INCLUDE,
      NON_JOB_KEY_STAGES,
      NON_JOB_KEY_WORKFLOW);

  public static final String JOB_KEY_SCRIPT = "script";
  public static final String JOB_KEY_STAGE = "stage";
  public static final String JOB_KEY_NEEDS = "needs";
  public static final String JOB_KEY_DOCKER = "docker";
  public static final String JOB_KEY_ARTIFACTS = "artifacts";
  public static final String JOB_KEY_PATHS = "paths";

  public static final String JOB_KEY_DOCKER_REGISTRY = "registry";
  public static final String JOB_KEY_ALLOW_FAILURE = "allow_failure";
  public static final String JOB_KEY_DOCKER_IMAGE = "image";
  public static final String DEFAULT_CONFIG_DIRECTORY = ".cicd-pipelines";
  public static final String YAML_EXTENSION = ".yml";
  public static final String YAML_ALT_EXTENSION = ".yaml";
}
