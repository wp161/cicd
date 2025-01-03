package neu.cs6510.pipelineservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import neu.cs6510.pipelineservice.service.ArgoCommandExecutionService;
import neu.cs6510.pipelineservice.service.ArgoYamlService;
import neu.cs6510.pipelineservice.service.PipelinePreparationService;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.ArgoLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PipelineControllerTest {

  @Mock
  private PipelinePreparationService pipelinePreparationService;

  @Mock
  private ArgoYamlService argoYamlService;

  @Mock
  private ArgoCommandExecutionService argoCommandExecutionService;
  @Mock
  private ArgoLogRepository argoLogRepository;

  @InjectMocks
  private PipelineController pipelineController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(pipelineController).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void testRunPipelineSuccess_WithConfigPath() throws Exception {
    Map<String, Object> requestParams = new HashMap<>();
    requestParams.put("repo_url", "https://github.com/example/repo");
    requestParams.put("branch", "main");
    requestParams.put("config_path", "path/to/config");
    requestParams.put("pipeline_name", null);

    Pipeline mockPipeline = mock(Pipeline.class);
    when(mockPipeline.getId()).thenReturn(1L);
    when(pipelinePreparationService.preparePipeline("https://github.com/example/repo", "main", "path/to/config", null))
        .thenReturn(mockPipeline);

    mockMvc.perform(post("/pipeline/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestParams)))
        .andExpect(status().isOk());

    verify(pipelinePreparationService, times(1))
        .preparePipeline("https://github.com/example/repo", "main", "path/to/config", null);
  }

  @Test
  void testRunPipelineSuccess_WithPipelineName() throws Exception {
    Map<String, Object> requestParams = new HashMap<>();
    requestParams.put("repo_url", "https://github.com/example/repo");
    requestParams.put("branch", "main");
    requestParams.put("config_path", null);
    requestParams.put("pipeline_name", "example-pipeline");

    Pipeline mockPipeline = mock(Pipeline.class);
    when(mockPipeline.getId()).thenReturn(2L);
    when(pipelinePreparationService.preparePipeline("https://github.com/example/repo", "main", null, "example-pipeline"))
        .thenReturn(mockPipeline);

    mockMvc.perform(post("/pipeline/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestParams)))
        .andExpect(status().isOk());

    verify(pipelinePreparationService, times(1))
        .preparePipeline("https://github.com/example/repo", "main", null, "example-pipeline");
  }

  @Test
  void testRunPipelineValidationError_MissingRepoUrl() throws Exception {
    Map<String, Object> requestParams = new HashMap<>();
    requestParams.put("branch", "main");
    requestParams.put("config_path", "path/to/config");

    mockMvc.perform(post("/pipeline/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestParams)))
        .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Validation error: Pipeline request must contain a non-empty string for repo URL"))
      .andExpect(jsonPath("$.status").value("error"));
  }

  @Test
  void testRunPipelineValidationError_MissingBothConfigPathAndPipelineName() throws Exception {
    Map<String, Object> requestParams = new HashMap<>();
    requestParams.put("repo_url", "https://github.com/example/repo");
    requestParams.put("branch", "main");

    mockMvc.perform(post("/pipeline/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestParams)))
        .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Validation error: One of configPath or pipelineName must be provided."))
      .andExpect(jsonPath("$.status").value("error"));  }

  @Test
  void testRunPipelineValidationError_BothConfigPathAndPipelineNameProvided() throws Exception {
    Map<String, Object> requestParams = new HashMap<>();
    requestParams.put("repo_url", "https://github.com/example/repo");
    requestParams.put("branch", "main");
    requestParams.put("config_path", "path/to/config");
    requestParams.put("pipeline_name", "example-pipeline");

    mockMvc.perform(post("/pipeline/run")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestParams)))
        .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Validation error: Only one of configPath or pipelineName should be provided, not both."))
      .andExpect(jsonPath("$.status").value("error"));
  }
}
