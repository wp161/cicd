package neu.cs6510.shared.repository;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import neu.cs6510.shared.entity.ArgoLog;
import neu.cs6510.shared.entity.Pipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ArgoLogRepositoryTest {

  @Mock
  private ArgoLogRepository argoLogRepository;

  private ArgoLog log1;
  private ArgoLog log2;
  private Pipeline pipeline;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    pipeline = Pipeline.builder()
        .id(1L)
        .name("Test Pipeline")
        .build();

    log1 = ArgoLog.builder()
        .id(1L)
        .pipelineId(pipeline)
        .argoWorkflowName("workflow1")
        .stageName("Build")
        .startTime(new Timestamp(System.currentTimeMillis() - 10000))
        .endTime(new Timestamp(System.currentTimeMillis()))
        .status("COMPLETED")
        .build();

    log2 = ArgoLog.builder()
        .id(2L)
        .pipelineId(pipeline)
        .argoWorkflowName("workflow1")
        .stageName("Test")
        .startTime(new Timestamp(System.currentTimeMillis() - 20000))
        .endTime(new Timestamp(System.currentTimeMillis()))
        .status("RUNNING")
        .build();
  }

  @Test
  void testFindByArgoWorkflowName() {
    when(argoLogRepository.findByArgoWorkflowName("workflow1")).thenReturn(Arrays.asList(log1, log2));

    List<ArgoLog> result = argoLogRepository.findByArgoWorkflowName("workflow1");

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals("workflow1", result.get(0).getArgoWorkflowName());
    verify(argoLogRepository, times(1)).findByArgoWorkflowName("workflow1");
  }

  @Test
  void testFindByPipelineId() {
    when(argoLogRepository.findByPipelineId(pipeline)).thenReturn(Arrays.asList(log1, log2));

    List<ArgoLog> result = argoLogRepository.findByPipelineId(pipeline);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(pipeline, result.get(0).getPipelineId());
    verify(argoLogRepository, times(1)).findByPipelineId(pipeline);
  }

  @Test
  void testFindByStatus() {
    when(argoLogRepository.findByStatus("COMPLETED")).thenReturn(Arrays.asList(log1));

    List<ArgoLog> result = argoLogRepository.findByStatus("COMPLETED");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("COMPLETED", result.get(0).getStatus());
    verify(argoLogRepository, times(1)).findByStatus("COMPLETED");
  }

  @Test
  void testFindByStartTimeBetween() {
    Timestamp start = new Timestamp(System.currentTimeMillis() - 20000);
    Timestamp end = new Timestamp(System.currentTimeMillis());

    when(argoLogRepository.findByStartTimeBetween(start, end)).thenReturn(Arrays.asList(log1, log2));

    List<ArgoLog> result = argoLogRepository.findByStartTimeBetween(start, end);

    assertNotNull(result);
    assertEquals(2, result.size());
    verify(argoLogRepository, times(1)).findByStartTimeBetween(start, end);
  }
}
