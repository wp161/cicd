package neu.cs6510.configservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.List;
import neu.cs6510.shared.entity.Job;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.entity.Stage;
import neu.cs6510.shared.repository.PipelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.yaml.snakeyaml.Yaml;


class ValidationServiceTest {
  @InjectMocks
  private ValidationService validationService;

  @Mock
  private PipelineRepository pipelineRepository;

  @Mock
  private Yaml yamlParser;

  @BeforeEach
  void setUp() {
    initMocks(this);
  }

  @Test
  void testParseAndValidateConfigFileSuccess() throws IOException {
    when(pipelineRepository.save(any(Pipeline.class))).thenAnswer(invocation ->
      invocation.getArgument(0));

    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContent.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");

    List<Stage> stages = pipeline.getStages();
    assertEquals(2, stages.size());

    Stage buildStage = stages.get(0);
    List<Job> jobs = buildStage.getJobs();
    assertEquals(2, jobs.size());

    Job buildJob = jobs.get(1);
    assertEquals("build", buildJob.getName());
    assertTrue(buildJob.getNeeds().contains("checkout"));

    String imageName = buildJob.getImageName();
    assertEquals("openjdk:17-jdk-slim", imageName);
  }

  @Test
  void testParsingAndValidateConfigFileSuccessWithDefaultDocker() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithDefaultDocker.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");

    List<Stage> stages = pipeline.getStages();
    assertEquals(2, stages.size());

    Stage buildStage = stages.get(0);
    List<Job> jobs = buildStage.getJobs();
    assertEquals(2, jobs.size());

    Job buildJob = jobs.get(1);
    assertEquals("build", buildJob.getName());
    assertTrue(buildJob.getNeeds().contains("checkout"));

    String imageName = buildJob.getImageName();
    assertEquals("openjdk:17-jdk-slim", imageName);
  }

  @Test
  void testParsingAndValidateConfigFileSuccessWithJobDocker() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithJobDocker.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");

    List<Stage> stages = pipeline.getStages();
    assertEquals(2, stages.size());

    Stage buildStage = stages.get(0);
    List<Job> jobs = buildStage.getJobs();
    Job buildJob = jobs.get(0);
    assertEquals("build", buildJob.getName());
    assertEquals("openjdk:21-jdk-slim", buildJob.getImageName());
    assertEquals("dockerhub.io", buildJob.getRegistry());
  }

  @Test
  void testParsingAndValidateConfigFileSuccessWithArtifact() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithArtifact.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");


    List<Stage> stages = pipeline.getStages();
    Stage buildStage = stages.get(0);
    List<Job> jobs = buildStage.getJobs();
    Job buildJob = jobs.get(0);
    assertEquals("build", buildJob.getName());
    assertTrue(buildJob.getPaths().contains("app/build/classes/*"));
  }

  @Test
  void testParsingAndValidateConfigFileSuccessWithDefaultPaths() throws IOException {

    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithDefaultPaths.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");

    List<Stage> stages = pipeline.getStages();

    Stage buildStage = stages.get(0);
    List<Job> jobs = buildStage.getJobs();
    Job buildJob = jobs.get(0);
    assertEquals("build", buildJob.getName());
    assertTrue(buildJob.getPaths().contains("app/build/reports/*"));
  }

  @Test
  void testParsingAndValidateConfigFileSuccessWithAllowFailure() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithAllowFailure.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");

    List<Stage> stages = pipeline.getStages();

    Stage buildStage = stages.get(0);
    List<Job> jobs = buildStage.getJobs();
    Job buildJob = jobs.get(0);
    assertEquals("build", buildJob.getName());
    assertTrue(buildJob.isAllowFailure());
  }

  @Test
  void testParseAndValidateConfigFileFailWithNoStage() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithNoStage.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithNullList() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithNullList.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithEmptyList() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithEmptyList.yaml";
    File file = new File(filePath);
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithWrongListType() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithWrongListType.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithNoPipelineName() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithNoPipelineName.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithStringScript() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithStringScript.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithNoDockerName() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithNoDockerName.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithNoDockerRegistry() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithNoDockerRegistry.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithCycleDependency() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithCycleDependency.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileSuccessWithoutStages() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithoutStages.yaml";
    File file = new File(filePath);
    Pipeline pipeline = validationService.parseAndValidateConfigFile(file, "repoUrl");

    List<Stage> stages = pipeline.getStages();
    assertEquals(4, stages.size());
  }

  @Test
  void testParseAndValidateConfigFileFailWithCrossStageNeeds() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithCrossStageNeeds.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithUndefinedNeeds() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithUndefinedNeeds.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithNullValue() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithNullValue.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithDuplicateJobs() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithDuplicateJobs.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithoutScript() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithoutScript.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithInvalidJobConfig() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithInvalidJobConfig.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithUndeclaredStage() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithUndeclaredStage.yaml";
    File file = new File(filePath);

    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }

  @Test
  void testParseAndValidateConfigFileFailWithDuplicateStages() throws IOException {
    String filePath = "src/test/java/neu/cs6510/configservice/testFiles/yamlContentWithDuplicateStages.yaml";
    File file = new File(filePath);
    RuntimeException exception = assertThrows(RuntimeException.class, () -> {
      validationService.parseAndValidateConfigFile(file, "repoUrl");
    });
    System.out.println(exception.getMessage());
  }
}