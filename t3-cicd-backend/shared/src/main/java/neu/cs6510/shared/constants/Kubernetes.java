package neu.cs6510.shared.constants;

public class Kubernetes {
  public static final String PV_PATH = "/mnt/git-repo/";
  public static final String PV_ARGO_PATH = "/mnt/argoworkflows/";
  public static final String CONFIGSERVICEURL =
    "http://configservice.t3cicdbackend.svc.cluster.local:8080";
  public static final String VALIDATEAPI = "/validate";
}
