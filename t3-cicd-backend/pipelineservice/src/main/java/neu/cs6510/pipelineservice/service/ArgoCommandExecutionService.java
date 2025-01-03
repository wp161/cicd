package neu.cs6510.pipelineservice.service;
import java.util.LinkedHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import neu.cs6510.shared.entity.ArgoLog;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.ArgoLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;

/**
 * Service for executing Argo Workflow commands.
 * This service provides methods to submit workflows to the Argo system and handle command execution.
 */
@Slf4j
@Service
public class ArgoCommandExecutionService {
  @Autowired
  private ArgoLogRepository argoLogRepository;

  /**
   * Method that will run the command to submit an argo workflow
   * @param filePath the file path of the argo-workflow.yaml
   * @throws IOException if there is a problem with the input or output
   * @throws InterruptedException if the process is interrupted
   */
  public void submitWorkflow(String filePath) throws IOException, InterruptedException {
    String command = "argo submit -n argo --watch " + filePath;
    launchCommand(command);
  }

  /**
   * Helper method to run a specific command
   * @param command to be run on bash
   * @throws IOException if there is a problem with the input or output
   * @throws InterruptedException if the process is interrupted
   */
  private void launchCommand(String command) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        log.info(line);
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      log.error("Command failed with exit code: {}", exitCode);
    } else {
      log.info("Command executed successfully.");
    }
  }

  /**
   * Fetch logs for the given Argo pipeline and save them to the database.
   *
   * @param pipelineId The ID of the pipeline
   * @param argoWorkflowName The name of the Argo Workflow
   * @throws IOException If there is an issue with reading the logs
   * @throws InterruptedException If the log process is interrupted
   */
  public void fetchAndSavePipelineLogs(Long pipelineId, String argoWorkflowName) throws IOException, InterruptedException {
    Pipeline pipeline = Pipeline.builder().id(pipelineId).build();
    String command = String.format("argo logs %s -n argo", argoWorkflowName);
    log.info("Executing command: {}", command);

    ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();

    Map<String, ArgoLog> stageLogs = new LinkedHashMap<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        log.info(line);

        if (line.contains("capturing logs")) {
          // Detect stage start
          String podName = extractPodName(line);
          ArgoLog logEntry = ArgoLog.builder()
              .pipelineId(pipeline)
              .argoWorkflowName(argoWorkflowName)
              .startTime(new Timestamp(System.currentTimeMillis()))
              .status("RUNNING")
              .build();
          stageLogs.put(podName, logEntry);
        } else if (!line.contains("msg=")) {
          // Detect stage name
          String podName = extractPodName(line);
          ArgoLog logEntry = stageLogs.get(podName);
          if (logEntry != null) {
            String rawStageName = line.split(":")[1].trim();
            // Clean up escape characters
            String cleanedStageName = rawStageName.replaceAll("\\u001B\\[[;\\d]*m", "");
            logEntry.setStageName(cleanedStageName);
          }
        } else if (line.contains("sub-process exited")) {
          // Detect stage completion
          String podName = extractPodName(line);
          ArgoLog logEntry = stageLogs.get(podName);
          if (logEntry != null) {
            logEntry.setEndTime(new Timestamp(System.currentTimeMillis()));
            logEntry.setStatus("COMPLETED");
            argoLogRepository.save(logEntry);
            stageLogs.remove(podName);
          }
        }
      }
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      log.error("Failed to fetch logs for pipeline {}: exit code {}", argoWorkflowName, exitCode);
    }
  }

  /**
   * Extracts the pod name from a log line.
   *
   * @param line the log line containing the pod name and other details.
   * @return the cleaned pod name without escape characters.
   */
  private String extractPodName(String line) {
    String rawPodName = line.split(":")[0].trim();
    return rawPodName.replaceAll("\\u001B\\[[;\\d]*m", "");
  }
}
