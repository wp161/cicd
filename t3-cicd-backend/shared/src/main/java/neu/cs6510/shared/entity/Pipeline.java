package neu.cs6510.shared.entity;

import static neu.cs6510.shared.constants.Pipeline.STATUS_PENDING;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a pipeline in the CI/CD process, containing a list of stages.
 * The pipeline manages the ordered execution of stages.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "pipelines")
public class Pipeline {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Name of the pipeline.
   */
  @Column(nullable = false)
  private String name;

  /**
   * Directory of the repo on Persistent Volume
   */
  @Column(name = "repo_dir")
  private String repoDir;

  /**
   * Path to the pipeline config file on Persistent Volume
   */
  @Column(name = "config_file_path")
  private String configFilePath;

  /**
   * Path to the argo workflow YAML file on Persistent Volume
   */
  @Column(name = "argo_yaml_path")
  private String argoYamlPath;

  /**
   * List of stages in the pipeline, representing different phases (e.g., build, test, deploy).
   * Defaults to an empty list if not explicitly set.
   */
  @OneToMany(mappedBy = "pipeline", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Stage> stages = new ArrayList<>();

  /**
   * URL of the repo.
   */
  @Column(name = "repo_url", nullable = false)
  private String repoUrl;

  /**
   * Start time of the pipeline execution.
   */
  @Column(name = "start_time")
  private Timestamp startTime;

  /**
   * End time of the pipeline execution.
   */
  @Column(name = "end_time")
  private Timestamp endTime;

  /**
   * Status of the pipeline, e.g., "SUCCESS", "FAILED", "RUNNING".
   */
  @Column(name = "status")
  @Builder.Default
  private String status = STATUS_PENDING;


  /**
   * Adds a stage to the pipeline.
   *
   * @param stage the stage to be added to the pipeline
   */
  public void addStage(Stage stage) {
    stages.add(stage);
  }

}
