package neu.cs6510.shared.constants;

/**
 * A class that holds various constant used mainly for Docker.
 * */
public class Docker {
  public static final String DOCKER_HOST = "tcp://localhost:2375";
  public static final String DOCKERHUB_REGISTRY = "docker.io";
  public static final String SIDECAR_IMAGE_NAME = "cicd-sidecar:latest";
  public static final String SIDE_CAR_CONTAINER_NAME = "code-sidecar";
  public static final String VOLUME_NAME = "code-volume";
  public static final Integer MONITORING_INTERVAL = 500;
}
