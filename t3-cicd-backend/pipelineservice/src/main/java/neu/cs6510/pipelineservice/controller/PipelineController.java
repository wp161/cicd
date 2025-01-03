package neu.cs6510.pipelineservice.controller;

import static neu.cs6510.shared.constants.RequestParameter.BRANCH;
import static neu.cs6510.shared.constants.RequestParameter.CONFIGPATH;
import static neu.cs6510.shared.constants.RequestParameter.PIPELINENAME;
import static neu.cs6510.shared.constants.RequestParameter.REPOURL;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import neu.cs6510.pipelineservice.service.ArgoCommandExecutionService;
import neu.cs6510.pipelineservice.service.ArgoYamlService;
import neu.cs6510.pipelineservice.service.PipelinePreparationService;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.ArgoLogRepository;
import neu.cs6510.shared.repository.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PipelineController {
  @Autowired
  private final PipelinePreparationService pipelinePreparationService;
  private final ArgoCommandExecutionService argoCommandExecutionService;
  private final ArgoYamlService argoYamlService;
  private final ArgoLogRepository argoLogRepository;
  private final PipelineRepository pipelineRepository;

  public PipelineController(PipelinePreparationService pipelinePreparationService,
      ArgoCommandExecutionService argoCommandExecutionService, ArgoYamlService argoYamlService,
      ArgoLogRepository argoLogRepository, PipelineRepository pipelineRepository) {
    this.pipelinePreparationService = pipelinePreparationService;
    this.argoCommandExecutionService = argoCommandExecutionService;
    this.argoYamlService = argoYamlService;
    this.argoLogRepository = argoLogRepository;
    this.pipelineRepository = pipelineRepository;
  }

  /**
   * Executes a pipeline job based on the provided configuration parameters.
   * This method performs the following steps:
   * <ol>
   *   <li>Validates the input parameters.</li>
   *   <li>Prepares the pipeline configuration using the provided Git repository, branch, and configuration path or pipeline name.</li>
   *   <li>Generates an Argo Workflow YAML file for the pipeline.</li>
   *   <li>Submits the workflow to Argo for execution.</li>
   *   <li>Monitors the workflow execution, fetching logs for each stage of the pipeline.</li>
   *   <li>Saves the logs and workflow status to the database.</li>
   * </ol>
   *
   * @param requestParams a map containing the following key-value pairs:
   *                      <ul>
   *                        <li>{@code repo_url} (String): A non-empty URL of the Git repository.</li>
   *                        <li>{@code branch} (String): A non-empty name of the branch in the repository.</li>
   *                        <li>{@code config_path} (String, optional): The path to the configuration file.
   *                            Either {@code config_path} or {@code pipeline_name} must be provided.</li>
   *                        <li>{@code pipeline_name} (String, optional): The name of the pipeline to use.
   *                            Either {@code config_path} or {@code pipeline_name} must be provided.</li>
   *                      </ul>
   * @return a {@link ResponseEntity} containing a JSON response represented as {@code Map<String, String>} with the following keys:
   *         <ul>
   *           <li>{@code status} (String): The status of the request (e.g., "success" or "error").</li>
   *           <li>{@code message} (String): A detailed message regarding the result of the operation.
   *               For successful requests, this includes the pipeline ID, status, and completion time.
   *               For failed requests, this includes the error message.</li>
   *         </ul>
   *         HTTP Status Codes:
   *         <ul>
   *           <li>{@code 200 OK}: If the pipeline was successfully deployed and logs were stored successfully.</li>
   *           <li>{@code 400 Bad Request}: If there was an error in validation or during pipeline execution.</li>
   *         </ul>
   * @throws IllegalArgumentException if the input parameters are invalid.
   * @throws Exception for general errors during pipeline preparation, execution, or logging.
   *
   * @apiNote This method records the following details for each pipeline stage:
   * <ul>
   *   <li>Start time: Captured when the stage begins execution.</li>
   *   <li>Stage name: Extracted from the Argo Workflow logs.</li>
   *   <li>End time: Captured when the stage completes.</li>
   *   <li>Status: Indicates whether the stage execution was successful (e.g., "COMPLETED").</li>
   * </ul>
   */
  @PostMapping("/pipeline/run")
  public ResponseEntity<Map<String, String>> runPipeline(@RequestBody Map<String, Object> requestParams) {
    log.info("Received request to run pipeline with parameters: {}", requestParams);
    try {
      validateRequest(requestParams);
      log.debug("Request parameters validated successfully.");
      String repoUrl = (String) requestParams.get(REPOURL);
      String branch = (String) requestParams.get(BRANCH);
      String configPath = (String) requestParams.get(CONFIGPATH);
      String pipelineName = (String) requestParams.get(PIPELINENAME);
      log.info("Preparing pipeline with repo URL: {}, branch: {}, configPath: {}, pipelineName: {}",
          repoUrl, branch, configPath, pipelineName);

      Pipeline pipeline = pipelinePreparationService.preparePipeline(repoUrl, branch, configPath,
          pipelineName);
      // Generate argo yaml file
      Map<String, String> result = argoYamlService.generateWorkflowYaml(pipeline);
      String argoYamlPath = result.get("filePath");
      String workflowName = result.get("workflowName");
      pipelinePreparationService.updateStartTimeAndStatus(pipeline.getId(),
          new Timestamp(System.currentTimeMillis()), "STARTED");
      // Submit argo workflow
      argoCommandExecutionService.submitWorkflow(argoYamlPath);
      log.info("Pipeline prepared successfully. Pipeline ID: {}", pipeline.getId());
      // Fetch and save argo workflow logs into DB
      argoCommandExecutionService.fetchAndSavePipelineLogs(pipeline.getId(), workflowName);
      List savedArgoLogs = argoLogRepository.findByArgoWorkflowName(workflowName);
      Timestamp completionTime = new Timestamp(System.currentTimeMillis());
      pipelinePreparationService.updateEndTimeAndStatus(pipeline.getId(),
          completionTime, "SUCCESS");
      log.info("Saved logs successful: {}", savedArgoLogs);
      Map<String, String> response = Map.of(
        "status", "success",
        "pipelineId", String.valueOf(pipeline.getId()),
        "completionTime", completionTime.toString()
      );
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      log.warn("Validation error: {}", e.getMessage());
      Map<String, String> response = Map.of(
        "status", "error",
        "message", "Validation error: " + e.getMessage()
      );
      return ResponseEntity.badRequest().body(response);    } catch (Exception e) {
      log.error("Error while running pipeline: {}", e.getMessage(), e);
      Map<String, String> response = Map.of(
        "status", "error",
        "message", "Error: " + e.getMessage()
      );
      return ResponseEntity.badRequest().body(response);    }
  }

  /**
   * Validates the request parameters for pipeline run.
   *
   * @param requestParams the map of validation request parameters.
   * @throws IllegalArgumentException if required fields are missing, empty, or invalid.
   */
  static void validateRequest(Map<String, Object> requestParams) {
    log.debug("Validating request parameters...");
    if (requestParams == null || requestParams.isEmpty()) {
      throw new IllegalArgumentException("Pipeline request is empty");
    }
    if (!requestParams.containsKey(REPOURL)
        || !(requestParams.get(REPOURL) instanceof String)
        || ((String) requestParams.get(REPOURL)).isEmpty()) {
      throw new IllegalArgumentException("Pipeline request must contain a non-empty string for repo URL");
    }
    if (!requestParams.containsKey(BRANCH)
        || !(requestParams.get(BRANCH) instanceof String)
        || ((String) requestParams.get(BRANCH)).isEmpty()) {
      throw new IllegalArgumentException("Pipeline request must contain a non-empty string for branch name");
    }
    String configPath = (String) requestParams.get(CONFIGPATH);
    String pipelineName = (String) requestParams.get(PIPELINENAME);
    if (configPath == null && pipelineName == null) {
      throw new IllegalArgumentException("One of configPath or pipelineName must be provided.");
    } else if (configPath != null && pipelineName != null) {
      throw new IllegalArgumentException("Only one of configPath or pipelineName should be provided, not both.");
    }
    log.debug("Request parameters are valid.");
  }
}

