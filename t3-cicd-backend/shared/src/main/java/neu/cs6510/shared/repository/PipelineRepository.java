package neu.cs6510.shared.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import neu.cs6510.shared.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PipelineRepository extends JpaRepository<Pipeline, Long> {
  // Find Pipeline by the id
  Optional<Pipeline> findById(Long id);

  // Find Pipelines by the repository URL
  List<Pipeline> findByRepoUrl(String repoUrl);

  // Find Pipelines by status
  List<Pipeline> findByStatus(String status);

  // Find Pipeline by the repository URL and pipeline name
  Pipeline findByRepoUrlAndName(String repoUrl, String name);

  // Find Pipelines by range of startTime
  List<Pipeline> findByStartTimeBetween(Timestamp start, Timestamp end);

  // Find Pipelines by range of endTime
  List<Pipeline> findByEndTimeBetween(Timestamp start, Timestamp end);

  // Find Pipelines by repository URL and range of startTime
  List<Pipeline> findByRepoUrlAndStartTimeBetween(String repoUrl, Timestamp start, Timestamp end);

  // Find Pipelines by repository URL and status
  List<Pipeline> findByRepoUrlAndStatus(String repoUrl, String status);

  // Find the most recent Pipeline by startTime
  Optional<Pipeline> findTopByOrderByStartTimeDesc();

}
