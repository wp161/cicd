package neu.cs6510.shared.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.entity.Stage;
import neu.cs6510.shared.repository.StageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class StageRepositoryTest {

  @Mock
  private StageRepository stageRepository;

  private Stage stage1;
  private Stage stage2;
  private Pipeline pipeline;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    pipeline = Pipeline.builder()
      .id(1L)
      .repoUrl("https://github.com/wp161/cicd-localrepo.git")
      .name("pipeline1")
      .build();

    stage1 = Stage.builder()
      .pipeline(pipeline)
      .name("build")
      .status("SUCCESS")
      .startTime(Timestamp.from(Instant.parse("2024-01-01T10:00:00Z")))
      .endTime(Timestamp.from(Instant.parse("2024-01-01T10:30:00Z")))
      .build();

    stage2 = Stage.builder()
      .pipeline(pipeline)
      .name("test")
      .status("RUNNING")
      .startTime(Timestamp.from(Instant.parse("2024-01-02T11:00:00Z")))
      .endTime(Timestamp.from(Instant.parse("2024-01-02T11:30:00Z")))
      .build();
  }

  @Test
  void testFindByPipelineId() {
    when(stageRepository.findByPipelineId(1L)).thenReturn(Arrays.asList(stage1, stage2));

    List<Stage> result = stageRepository.findByPipelineId(1L);

    verify(stageRepository, times(1)).findByPipelineId(1L);
    assertEquals(2, result.size());
    assertEquals(stage1, result.get(0));
    assertEquals(stage2, result.get(1));
  }

  @Test
  void testFindByPipelineIdAndName() {
    when(stageRepository.findByPipelineIdAndName(1L, "build")).thenReturn(stage1);

    Stage result = stageRepository.findByPipelineIdAndName(1L, "build");

    verify(stageRepository, times(1)).findByPipelineIdAndName(1L, "build");
    assertEquals(stage1, result);
  }

  @Test
  void testFindByStatus() {
    when(stageRepository.findByStatus("SUCCESS")).thenReturn(List.of(stage1));

    List<Stage> result = stageRepository.findByStatus("SUCCESS");

    verify(stageRepository, times(1)).findByStatus("SUCCESS");
    assertEquals(1, result.size());
    assertEquals(stage1, result.get(0));
  }

  @Test
  void testFindByStartTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T09:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T11:00:00Z"));

    when(stageRepository.findByStartTimeBetween(start, end)).thenReturn(List.of(stage1));

    List<Stage> result = stageRepository.findByStartTimeBetween(start, end);

    verify(stageRepository, times(1)).findByStartTimeBetween(start, end);
    assertEquals(1, result.size());
    assertEquals(stage1, result.get(0));
  }

  @Test
  void testFindByEndTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T12:00:00Z"));

    when(stageRepository.findByEndTimeBetween(start, end)).thenReturn(List.of(stage1));

    List<Stage> result = stageRepository.findByEndTimeBetween(start, end);

    verify(stageRepository, times(1)).findByEndTimeBetween(start, end);
    assertEquals(1, result.size());
    assertEquals(stage1, result.get(0));
  }

  @Test
  void testFindByPipelineIdAndStatus() {
    when(stageRepository.findByPipelineIdAndStatus(1L, "SUCCESS")).thenReturn(List.of(stage1));

    List<Stage> result = stageRepository.findByPipelineIdAndStatus(1L, "SUCCESS");

    verify(stageRepository, times(1)).findByPipelineIdAndStatus(1L, "SUCCESS");
    assertEquals(1, result.size());
    assertEquals(stage1, result.get(0));
  }

  @Test
  void testFindByPipelineIdAndStartTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T09:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T11:00:00Z"));

    when(stageRepository.findByPipelineIdAndStartTimeBetween(1L, start, end)).thenReturn(List.of(stage1));

    List<Stage> result = stageRepository.findByPipelineIdAndStartTimeBetween(1L, start, end);

    verify(stageRepository, times(1)).findByPipelineIdAndStartTimeBetween(1L, start, end);
    assertEquals(1, result.size());
    assertEquals(stage1, result.get(0));
  }

  @Test
  void testFindByPipelineIdAndEndTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-02T12:00:00Z"));

    when(stageRepository.findByPipelineIdAndEndTimeBetween(1L, start, end)).thenReturn(List.of(stage1, stage2));

    List<Stage> result = stageRepository.findByPipelineIdAndEndTimeBetween(1L, start, end);

    verify(stageRepository, times(1)).findByPipelineIdAndEndTimeBetween(1L, start, end);
    assertEquals(2, result.size());
    assertEquals(stage1, result.get(0));
    assertEquals(stage2, result.get(1));
  }

  @Test
  void testFindTopByPipelineIdOrderByStartTimeDesc() {
    when(stageRepository.findTopByPipelineIdOrderByStartTimeDesc(1L)).thenReturn(stage2);

    Stage result = stageRepository.findTopByPipelineIdOrderByStartTimeDesc(1L);

    verify(stageRepository, times(1)).findTopByPipelineIdOrderByStartTimeDesc(1L);
    assertEquals(stage2, result);
  }

  @Test
  void testFindTopByPipelineIdOrderByStartTimeAsc() {
    when(stageRepository.findTopByPipelineIdOrderByStartTimeAsc(1L)).thenReturn(stage1);

    Stage result = stageRepository.findTopByPipelineIdOrderByStartTimeAsc(1L);

    verify(stageRepository, times(1)).findTopByPipelineIdOrderByStartTimeAsc(1L);
    assertEquals(stage1, result);
  }

  @Test
  void testFindByPipelineIdOrderByStartTimeAsc() {
    when(stageRepository.findByPipelineIdOrderByStartTimeAsc(1L)).thenReturn(Arrays.asList(stage1, stage2));

    List<Stage> result = stageRepository.findByPipelineIdOrderByStartTimeAsc(1L);

    verify(stageRepository, times(1)).findByPipelineIdOrderByStartTimeAsc(1L);
    assertEquals(2, result.size());
    assertEquals(stage1, result.get(0));
    assertEquals(stage2, result.get(1));
  }
}
