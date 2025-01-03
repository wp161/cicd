package neu.cs6510.shared.repository;

import java.sql.Timestamp;
import neu.cs6510.shared.entity.ArgoLog;
import neu.cs6510.shared.entity.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for accessing and managing ArgoLog entities in the database.
 * Extends JpaRepository to leverage standard CRUD operations and custom query methods.
 */
@Repository
public interface ArgoLogRepository extends JpaRepository<ArgoLog, Long> {

  // Find all ArgoLog entries by the associated Argo workflow name.
  List<ArgoLog> findByArgoWorkflowName(String argoWorkflowName);

  // Find all ArgoLog entries by the pipeline ID.
  List<ArgoLog> findByPipelineId(Pipeline pipelineId);

  // Find all ArgoLog entries by the stage name.
  List<ArgoLog> findByStageName(String stageName);

  // Find all ArgoLog entries by status.
  List<ArgoLog> findByStatus(String status);

  // Find all ArgoLog entries by workflow name and status.
  List<ArgoLog> findByArgoWorkflowNameAndStatus(String argoWorkflowName, String status);

  // Find all ArgoLog entries by pipeline ID and status.
  List<ArgoLog> findByPipelineIdAndStatus(Pipeline pipelineId, String status);

   // Find all ArgoLog entries by the start time range.
  List<ArgoLog> findByStartTimeBetween(Timestamp start, Timestamp end);

   // Find all ArgoLog entries by the end time range.
  List<ArgoLog> findByEndTimeBetween(Timestamp start, Timestamp end);

   // Find all ArgoLog entries by workflow name and start time range.
  List<ArgoLog> findByArgoWorkflowNameAndStartTimeBetween(String argoWorkflowName, Timestamp start, Timestamp end);

}
