package neu.cs6510.shared.entity;

import static neu.cs6510.shared.constants.Pipeline.STATUS_PENDING;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
 * The Stage class represents a stage within a CI/CD pipeline.
 * A stage can contain multiple jobs that must be executed.
 * The execution of jobs within a stage is parallel unless dependencies are specified.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stages")
public class Stage {

  /**
   * The unique identifier of the stage (for database persistence).
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The pipeline that the stage belongs to
   */
  @ManyToOne
  @JoinColumn(name = "pipeline_id", nullable = false)
  @EqualsAndHashCode.Exclude // Prevents recursive hashCode call
  @ToString.Exclude          // Prevents recursive toString call
  private Pipeline pipeline;

  /**
   * The name of the stage, such as 'build', 'test', 'deploy', etc.
   */
  @Column(name = "name", nullable = false)
  private String name;

  /**
   * A list of jobs that belong to this stage. The jobs are executed as part of this stage.
   */
  @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<Job> jobs = new ArrayList<>();

  /**
   * The current status of the stage (e.g., pending, running, failed, completed).
   */
  @Column(name = "status", nullable = false)
  @Builder.Default
  private String status = STATUS_PENDING;

  /**
   * The time when the stage starts.
   */
  @Column(name = "start_time")
  private Timestamp startTime;

  /**
   * The time when the stage completes.
   */
  @Column(name = "end_time")
  private Timestamp endTime;

  /**
   * Adds a job to the list of jobs in this stage.
   *
   * @param job the Job to add
   */
  public void addJob(Job job) {
    jobs.add(job);
  }

  /**
   * Checks whether the stage contains a job with the specified name.
   *
   * @param jobName the name of the job to search for
   * @return true if the job exists in the stage, otherwise false.
   */
  public boolean hasJob(String jobName) {
    return jobs.stream().anyMatch(job -> job.getName().equals(jobName));
  }

}

