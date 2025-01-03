package neu.cs6510.pipelineservice.service;

import static neu.cs6510.shared.constants.Kubernetes.PV_ARGO_PATH;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import neu.cs6510.shared.repository.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import neu.cs6510.shared.entity.Job;
import neu.cs6510.shared.entity.Pipeline;

@Slf4j
@Service
public class ArgoYamlService {

  @Autowired
  private PipelineRepository pipelineRepository;

  /**
   * Generates an Argo Workflow YAML file based on the provided pipeline configuration.
   * The YAML file includes metadata and specifications required to define the workflow.
   *
   * @param pipeline the {@link Pipeline} object containing the pipeline configuration,
   *                 including jobs and their dependencies.
   * @return a {@link Map} containing:
   *         <ul>
   *           <li>{@code yamlContent} - the generated YAML content as a string.</li>
   *           <li>{@code filePath} - the absolute path where the YAML file is saved.</li>
   *           <li>{@code workflowName} - the name of the generated workflow.</li>
   *         </ul>
   * @throws IOException if an error occurs while saving the YAML to a file.
   */
  public Map<String, String> generateWorkflowYaml(Pipeline pipeline) throws IOException {
    Map<String, Object> workflow = createWorkflow(pipeline);
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    options.setPrettyFlow(true);
    Yaml workflowYaml = new Yaml(options);
    String yaml = workflowYaml.dump(workflow);
    String absoluteYamlPath = saveWorkflowToFile(yaml);
    String workflowName = (String) ((Map<String, Object>) workflow.get("metadata")).get("name");

    Map<String, String> result = new HashMap<>();
    result.put("yamlContent", yaml);
    result.put("filePath", absoluteYamlPath);
    result.put("workflowName", workflowName);
    pipeline.setArgoYamlPath(absoluteYamlPath);
    pipelineRepository.save(pipeline);
    return result;
  }

  /**
   * Saves the provided YAML content to a uniquely named file in the Persistent Volume.
   *
   * @param yamlContent the YAML content to be written to the file.
   * @return the absolute file path of the saved YAML file in the Persistent Volume.
   * @throws IOException if an error occurs while creating the directory or writing to the file.
   */
  private String saveWorkflowToFile(String yamlContent) throws IOException {
    File directory = new File(PV_ARGO_PATH);
    if (!directory.exists()) {
        boolean created = directory.mkdirs();
        if (!created) {
            throw new IOException("Failed to create directory: " + PV_ARGO_PATH);
        }
    }

    String filePath = PV_ARGO_PATH + UUID.randomUUID() + "argo-workflow.yaml";
    File file = new File(filePath);
    try (FileWriter writer = new FileWriter(file)) {
        writer.write(yamlContent);
    }

    String absolutePath = file.getAbsolutePath();
    System.out.println("Workflow YAML saved to: " + absolutePath);
    return absolutePath;
  }

  /**
   * Helper method to create the argo workflow
   * @param pipeline that will be used to create the argo workflow
   * @return the mapping of the workflow
   */
  private Map<String, Object> createWorkflow(Pipeline pipeline) {
    // Root structure of the YAML file
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("apiVersion", "argoproj.io/v1alpha1");
    root.put("kind", "Workflow");

    // Metadata for the workflow
    Map<String, Object> metadata = new LinkedHashMap<>();
    String workflowName = "pipeline-" + pipeline.getName() + "-" + UUID.randomUUID();
    metadata.put("name", workflowName);
    root.put("metadata", metadata);

    // Workflow spec with entry point and templates
    Map<String, Object> spec = new LinkedHashMap<>();
    spec.put("entrypoint", "pipeline");

    // Create the Template
    List<Map<String, Object>> templates = new ArrayList<>();

    // Create the DAG Template
    Map<String, Object> pipelineTemplate = new LinkedHashMap<>();
    pipelineTemplate.put("name", "pipeline");
    pipelineTemplate.put("dag", generateStageDag(pipeline));
    templates.add(pipelineTemplate);

    // Generate DAG Templates for Each Stage
    generateStageTemplates(pipeline, templates);

    // Add job-specific templates
    generateJobTemplates(pipeline, templates);

    spec.put("templates", templates);
    root.put("spec", spec);
    return root;
  }

  /**
   * Helper method to generate the templates for each stage
   * @param pipeline that will be used to create the argo workflow
   * @param templates the list of templates to add each stage template to
   */
  private void generateStageTemplates(Pipeline pipeline, List<Map<String, Object>> templates) {
    for (var stage : pipeline.getStages()) {
      Map<String, Object> stageTemplate = new LinkedHashMap<>();
      stageTemplate.put("name", stage.getName() + "-dag");

      // Generate the DAG for jobs within this stage
      Map<String, Object> stageDag = new LinkedHashMap<>();
      List<Map<String, Object>> stageTasks = stage.getJobs().stream()
          .map(ArgoYamlService::createTask)
          .collect(Collectors.toList());
      stageDag.put("tasks", stageTasks);

      stageTemplate.put("dag", stageDag);
      templates.add(stageTemplate);
    }
  }

  /**
   * Helper method to generate the dag portion for each stage in the workflow
   * @param pipeline that will be used to create the argo workflow
   * @return the dag portion mapping for each stage
   */
  private Map<String, Object> generateStageDag(Pipeline pipeline) {
    Map<String, Object> dag = new LinkedHashMap<>();
    List<Map<String, Object>> tasks = new ArrayList<>();

    for (int i = 0; i < pipeline.getStages().size(); i++) {
      String stageName = pipeline.getStages().get(i).getName();
      Map<String, Object> task = new LinkedHashMap<>();
      task.put("name", stageName);
      task.put("template", stageName + "-dag");

      // Add dependencies for sequential execution
      if (i > 0) {
        task.put("dependencies", List.of(pipeline.getStages().get(i - 1).getName()));
      }

      tasks.add(task);
    }

    dag.put("tasks", tasks);
    return dag;
  }

  /**
   * Helper method to generate the templates for each job in the pipeline
   * @param pipeline that will be used to create the argo workflow
   * @param templates the list of templates to add each job template to
   */
  private void generateJobTemplates(Pipeline pipeline, List<Map<String, Object>> templates) {
    pipeline.getStages().stream()
        .flatMap(stage -> stage.getJobs().stream())
        .map(ArgoYamlService::createJobSpecificTemplate)
        .forEach(templates::add);
  }

  /**
   * Helper method to create a task for each job
   * @param job the job object to extract details from
   * @return the task mapping for a specific job
   */
  private static Map<String, Object> createTask(Job job) {
    Map<String, Object> task = new LinkedHashMap<>();
    task.put("name", job.getName());
    task.put("template", job.getName() + "-template");
    if (!job.getNeeds().isEmpty()) {
      task.put("dependencies", job.getNeeds());
    }
    List<Map<String, Object>> parameters = new ArrayList<>();
    parameters.add(Map.of("name", "script", "value", String.join("\n", job.getScripts())));
    Map<String, Object> arguments = Map.of("parameters", parameters);
    task.put("arguments", arguments);

    return task;
  }

  /**
   * Helper method to create the template for specific jobs
   * @param job the job object to extract details from
   * @return the job specific template mapping
   */
  private static Map<String, Object> createJobSpecificTemplate(Job job) {
    Map<String, Object> jobTemplate = new LinkedHashMap<>();
    jobTemplate.put("name", job.getName() + "-template");

    // Inputs
    Map<String, Object> inputs = new LinkedHashMap<>();
    inputs.put("parameters", List.of(Map.of("name", "script")));
    jobTemplate.put("inputs", inputs);

    // Container
    Map<String, Object> container = new LinkedHashMap<>();
    container.put("image", job.getImageName());
    container.put("command", List.of("sh", "-c"));
    container.put("args", List.of("{{inputs.parameters.script}}"));
    jobTemplate.put("container", container);

    return jobTemplate;
  }
}