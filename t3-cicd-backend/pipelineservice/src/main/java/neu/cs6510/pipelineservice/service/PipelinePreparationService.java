package neu.cs6510.pipelineservice.service;

import static neu.cs6510.shared.constants.Kubernetes.CONFIGSERVICEURL;
import static neu.cs6510.shared.constants.Kubernetes.VALIDATEAPI;
import static neu.cs6510.shared.constants.Pipeline.PIPELINE_ID;
import static neu.cs6510.shared.constants.Pipeline.STATUS_SUCCESS;
import static neu.cs6510.shared.constants.RequestParameter.BRANCH;
import static neu.cs6510.shared.constants.RequestParameter.CONFIGPATH;
import static neu.cs6510.shared.constants.RequestParameter.PIPELINENAME;
import static neu.cs6510.shared.constants.RequestParameter.REPOURL;
import static neu.cs6510.shared.constants.ResponseBody.REPODIR;

import jakarta.persistence.EntityNotFoundException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PipelinePreparationService {

  private final PipelineRepository pipelineRepository;
  private final RestTemplate restTemplate;

  @Autowired
  public PipelinePreparationService(PipelineRepository pipelineRepository,
    RestTemplate restTemplate) {
    this.pipelineRepository = pipelineRepository;
    this.restTemplate = restTemplate;
  }

  /**
   * Validates the configuration using the ConfigService and returns the pipeline ID if successful.
   *
   * @param repoUrl     The Git repository URL.
   * @param branch      The branch name for check pipeline validation.
   * @param configPath  The path to the configuration file (optional, mutually exclusive with pipelineName).
   * @param pipelineName The name of the pipeline to search for in the configuration (optional, mutually exclusive with configPath).
   * @return The pipelineId if validation is successful.
   * @throws RuntimeException If the validation fails or the service call encounters an error.
   */
  private Map<String, String> validateConfig(String repoUrl, String branch, String configPath,
    String pipelineName) {
    log.info("Validating configuration with repoUrl: {}, branch: {}, configPath: {}, "
      + "pipelineName: {}", repoUrl, branch, configPath, pipelineName);

    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put(REPOURL, repoUrl);
    requestBody.put(BRANCH, branch);
    if (configPath != null) {
      requestBody.put(CONFIGPATH, configPath);
    } else if (pipelineName != null) {
      requestBody.put(PIPELINENAME, pipelineName);
    }

    log.debug("Sending request to ConfigService at {} with body: {}",
      CONFIGSERVICEURL, requestBody);

    ResponseEntity<Map> response = restTemplate.postForEntity(CONFIGSERVICEURL + VALIDATEAPI,
      requestBody, Map.class);

    if (response.getStatusCode().is2xxSuccessful()) {
      log.debug("Received successful response from ConfigService: {}", response.getBody());
      Map<String, String> responseBody = (Map<String, String>) response.getBody();
      if (STATUS_SUCCESS.equals(responseBody.get("status"))) {
        log.info("Configuration validation succeeded. Pipeline ID: {}",
          responseBody.get(PIPELINE_ID));
        return responseBody;
      } else {
        log.warn("Configuration validation failed: {}", responseBody.get("message"));
        throw new RuntimeException("Config validation failed: " + responseBody.get("message"));
      }
    } else {
      log.error("Failed to call ConfigService. HTTP status: {}", response.getStatusCode());
      throw new RuntimeException("Failed to call ConfigService: HTTP " + response.getStatusCode());
    }
  }

  /**
   * Prepares a pipeline by validating its configuration and retrieving it from the database.
   *
   * @param repoUrl     The repository URL.
   * @param branch      The branch name.
   * @param configPath  The configuration file path (optional).
   * @param pipelineName The pipeline name (optional).
   * @return The prepared Pipeline object.
   */
  public Pipeline preparePipeline(String repoUrl, String branch, String configPath,
    String pipelineName) {
    log.info("Preparing pipeline with repoUrl: {}, branch: {}, configPath: {}, pipelineName: {}",
      repoUrl, branch, configPath, pipelineName);

    Map<String, String> response = validateConfig(repoUrl, branch, configPath, pipelineName);
    String pipelineId = response.get(PIPELINE_ID);
    log.debug("Validated configuration. Pipeline ID: {}", pipelineId);

    return pipelineRepository.findById(Long.parseLong(pipelineId))
        .orElseThrow(() -> {
          log.warn("Pipeline not found in database for ID: {}", pipelineId);
          return new RuntimeException("Pipeline not found after validation.");
        });
  }

  /**
   * Updates the start time and status of a Pipeline.
   *
   * @param pipelineId the ID of the Pipeline to update
   * @param startTime the new start time to set
   * @param status the new status to set
   * @throws EntityNotFoundException if no Pipeline with the given ID is found
   */
  @Transactional
  public void updateStartTimeAndStatus(Long pipelineId, Timestamp startTime, String status) {
    Pipeline pipeline = pipelineRepository.findById(pipelineId)
        .orElseThrow(() -> new EntityNotFoundException("Pipeline with id " + pipelineId + " not found"));

    pipeline.setStartTime(startTime);
    pipeline.setStatus(status);

    pipelineRepository.save(pipeline);
    log.info("Updated pipeline start time and status: id={}, startTime={}, status={}",
        pipelineId, startTime, status);
  }

  /**
   * Updates the end time and status of a Pipeline.
   *
   * @param pipelineId the ID of the Pipeline to update
   * @param endTime the new end time to set
   * @param status the new status to set
   * @throws EntityNotFoundException if no Pipeline with the given ID is found
   */
  @Transactional
  public void updateEndTimeAndStatus(Long pipelineId, Timestamp endTime, String status) {
    Pipeline pipeline = pipelineRepository.findById(pipelineId)
        .orElseThrow(() -> new EntityNotFoundException("Pipeline with id " + pipelineId + " not found"));

    pipeline.setEndTime(endTime);
    pipeline.setStatus(status);

    pipelineRepository.save(pipeline);
    log.info("Updated pipeline end time and status: id={}, endTime={}, status={}",
        pipelineId, endTime, status);
  }
}
