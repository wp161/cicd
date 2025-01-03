package neu.cs6510.shared.entity;

import static neu.cs6510.shared.constants.Pipeline.STATUS_PENDING;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * The Job class represents a unit of work within a CI/CD pipeline.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "jobs")
public class Job {

  /**
   * The unique identifier of the job (for database persistence).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The name of the job, which must be unique within a pipeline.
   */
  @Column(nullable = false)
  private String name;

  /**
   * The stage that the job belongs to
   */
  @ManyToOne
  @JoinColumn(name = "stage_id", nullable = false)
  @EqualsAndHashCode.Exclude // Prevents recursive hashCode call
  @ToString.Exclude          // Prevents recursive toString call
  private Stage stage;

  /**
   * The current status of the job (e.g., pending, running, failed, completed).
   */
  @Column(name = "status", nullable = false)
  @Builder.Default
  private String status = STATUS_PENDING;

  /**
   * A list of scripts that belong to this job. The scripts are used to execute the job.
   */
  @ElementCollection
  @CollectionTable(name = "job_scripts", joinColumns = @JoinColumn(name = "job_id"))
  @Column(name = "script")
  @Builder.Default
  private List<String> scripts = new ArrayList<>();

  /**
   * A list of name of jobs that this job depends on. The job needs to wait for all necessary jobs
   * to finish.
   */
  @ElementCollection
  @CollectionTable(name = "job_dependencies", joinColumns = @JoinColumn(name = "job_id"))
  @Column(name = "need")
  @Builder.Default
  private List<String> needs = new ArrayList<>();

  /**
   * The registry to get the image to run this job.
   */
  @Column(name = "registry")
  private String registry;

  /**
   * The image required running this job.
   */
  @Column(name = "image_name")
  private String imageName;

  /**
   * The time when the job starts.
   */
  @Column(name = "start_time")
  private Timestamp startTime;

  /**
   * The time when the job completes.
   */
  @Column(name = "end_time")
  private Timestamp completionTime;

  /**
   * Whether job failure is allowed, default is false
   */
  @Column(name = "allow_failure")
  @Builder.Default
  private boolean allowFailure = false;

  /**
   * Files, folders, patterns on paths and files to upload on completion of the task.
   */
  @ElementCollection
  @CollectionTable(name = "job_paths", joinColumns = @JoinColumn(name = "job_id"))
  @Column(name = "path")
  @Builder.Default  private List<String> paths = new ArrayList<>();
}
