package neu.cs6510.shared.repository;

import java.sql.Timestamp;
import java.util.List;
import neu.cs6510.shared.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

  // Find jobs by stage ID
  List<Job> findByStageId(Long stageId);

  // Find jobs by status
  List<Job> findByStatus(String status);

  // Find jobs by stage ID and status
  List<Job> findByStageIdAndStatus(Long stageId, String status);

  // Find jobs by registry
  List<Job> findByRegistry(String registry);

  // Find jobs by image name
  List<Job> findByImageName(String imageName);

  // Find jobs by start time range
  List<Job> findByStartTimeBetween(Timestamp start, Timestamp end);

  // Find jobs by completion time range
  List<Job> findByCompletionTimeBetween(Timestamp start, Timestamp end);

  // Find jobs by stage and start time range
  List<Job> findByStageIdAndStartTimeBetween(Long stageId, Timestamp start, Timestamp end);

  // Find jobs by stage and completion time range
  List<Job> findByStageIdAndCompletionTimeBetween(Long stageId, Timestamp start, Timestamp end);

  // Find jobs by allowing failure
  List<Job> findByAllowFailure(boolean allowFailure);

  // Find jobs by name within a specific stage (unique within a pipeline)
  Job findByStageIdAndName(Long stageId, String name);

  // Find the most recent job in a stage by start time
  Job findTopByStageIdOrderByStartTimeDesc(Long stageId);

  // Find the earliest job in a stage by start time
  Job findTopByStageIdOrderByStartTimeAsc(Long stageId);

  // Find all jobs in a stage ordered by start time
  List<Job> findByStageIdOrderByStartTimeAsc(Long stageId);
}
