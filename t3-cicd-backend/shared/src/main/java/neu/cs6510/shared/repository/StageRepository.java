package neu.cs6510.shared.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import neu.cs6510.shared.entity.Stage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StageRepository extends JpaRepository<Stage, Long> {
  // Find stage by the id
  Optional<Stage> findById(Long id);

  // Find stages by pipeline
  List<Stage> findByPipelineId(Long pipelineId);

  // Find stages by pipeline and stage name
  Stage findByPipelineIdAndName(Long pipelineId, String name);

  // Find stages by status
  List<Stage> findByStatus(String status);

  // Find stages by start time range
  List<Stage> findByStartTimeBetween(Timestamp start, Timestamp end);

  // Find stages by end time range
  List<Stage> findByEndTimeBetween(Timestamp start, Timestamp end);

  // Find stages by pipeline and status
  List<Stage> findByPipelineIdAndStatus(Long pipelineId, String status);

  // Find stages by pipeline and start time range
  List<Stage> findByPipelineIdAndStartTimeBetween(Long pipelineId, Timestamp start, Timestamp end);

  // Find stages by pipeline and end time range
  List<Stage> findByPipelineIdAndEndTimeBetween(Long pipelineId, Timestamp start, Timestamp end);

  // Find the latest stage in a pipeline by start time
  Stage findTopByPipelineIdOrderByStartTimeDesc(Long pipelineId);

  // Find the earliest stage in a pipeline by start time
  Stage findTopByPipelineIdOrderByStartTimeAsc(Long pipelineId);

  // Find all stages in a pipeline in the order of their start time
  List<Stage> findByPipelineIdOrderByStartTimeAsc(Long pipelineId);
}
