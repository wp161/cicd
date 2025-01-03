package neu.cs6510.pipelineservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.PipelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class PipelinePreparationServiceTest {

  @Mock
  private PipelineRepository pipelineRepository;

  @Mock
  private RestTemplate restTemplate;

  @InjectMocks
  private PipelinePreparationService pipelinePreparationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testPreparePipeline_Success() {
    String repoUrl = "https://github.com/example/repo";
    String branch = "main";
    String configPath = "path/to/config";
    String pipelineName = null;
    String pipelineId = "12345";

    Pipeline mockPipeline = new Pipeline();
    mockPipeline.setId(Long.parseLong(pipelineId));
    mockPipeline.setName("Test Pipeline");
    Map<String, String> responseBody = new HashMap<>();
    responseBody.put("status", "success");
    responseBody.put("pipelineId", pipelineId);
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));
    when(pipelineRepository.findById(Long.parseLong(pipelineId)))
        .thenReturn(Optional.of(mockPipeline));
    Pipeline result = pipelinePreparationService.preparePipeline(repoUrl, branch, configPath, pipelineName);

    assertNotNull(result);
    assertEquals(Long.parseLong(pipelineId), result.getId());
    assertEquals("Test Pipeline", result.getName());
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    verify(pipelineRepository, times(1)).findById(Long.parseLong(pipelineId));
  }

  @Test
  void testPreparePipeline_ConfigValidationFails() {
    String repoUrl = "https://github.com/example/repo";
    String branch = "main";
    String configPath = "path/to/config";
    String pipelineName = null;

    Map<String, String> responseBody = new HashMap<>();
    responseBody.put("status", "failure");
    responseBody.put("message", "Validation error");
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        pipelinePreparationService.preparePipeline(repoUrl, branch, configPath, pipelineName)
    );

    assertEquals("Config validation failed: Validation error", exception.getMessage());
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    verify(pipelineRepository, never()).findById(anyLong());
  }

  @Test
  void testPreparePipeline_PipelineNotFound() {
    String repoUrl = "https://github.com/example/repo";
    String branch = "main";
    String configPath = "path/to/config";
    String pipelineName = null;
    String pipelineId = "12345";

    Map<String, String> responseBody = new HashMap<>();
    responseBody.put("status", "success");
    responseBody.put("pipelineId", pipelineId);
    when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
        .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));
    when(pipelineRepository.findById(Long.parseLong(pipelineId)))
        .thenReturn(Optional.empty());

    RuntimeException exception = assertThrows(RuntimeException.class, () ->
        pipelinePreparationService.preparePipeline(repoUrl, branch, configPath, pipelineName)
    );

    assertEquals("Pipeline not found after validation.", exception.getMessage());
    verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(Map.class));
    verify(pipelineRepository, times(1)).findById(Long.parseLong(pipelineId));
  }
}
