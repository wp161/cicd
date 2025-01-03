package neu.cs6510.shared.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.PipelineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PipelineRepositoryTest {

  @Mock
  private PipelineRepository pipelineRepository;

  private Pipeline pipeline1;
  private Pipeline pipeline2;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    pipeline1 = Pipeline.builder()
      .repoUrl("https://github.com/wp161/cicd-localrepo.git")
      .name("pipeline1")
      .status("SUCCESS")
      .startTime(Timestamp.from(Instant.parse("2024-01-01T10:00:00Z")))
      .endTime(Timestamp.from(Instant.parse("2024-01-01T10:30:00Z")))
      .build();
    pipeline2 = Pipeline.builder()
      .repoUrl("https://github.com/wp161/cicd-localrepo.git")
      .name("pipeline2")
      .status("RUNNING")
      .startTime(Timestamp.from(Instant.parse("2024-01-02T11:00:00Z")))
      .endTime(Timestamp.from(Instant.parse("2024-01-02T11:30:00Z")))
      .build();
  }

  @Test
  void testFindByRepoUrl() {
    when(pipelineRepository.findByRepoUrl("https://github.com/wp161/cicd-localrepo.git"))
      .thenReturn(Arrays.asList(pipeline1, pipeline2));

    List<Pipeline> result = pipelineRepository
      .findByRepoUrl("https://github.com/wp161/cicd-localrepo.git");

    verify(pipelineRepository, times(1))
      .findByRepoUrl("https://github.com/wp161/cicd-localrepo.git");
    assertEquals(2, result.size());
    assertEquals(pipeline1, result.get(0));
    assertEquals(pipeline2, result.get(1));
  }

  @Test
  void testFindByRepoUrlAndName() {
    when(pipelineRepository.findByRepoUrlAndName("https://github.com/wp161/cicd-localrepo.git", "pipeline1"))
      .thenReturn(pipeline1);

    Pipeline result = pipelineRepository
      .findByRepoUrlAndName("https://github.com/wp161/cicd-localrepo.git", "pipeline1");

    verify(pipelineRepository, times(1))
      .findByRepoUrlAndName("https://github.com/wp161/cicd-localrepo.git", "pipeline1");
    assertEquals(pipeline1, result);
  }

  @Test
  void testFindById() {
    when(pipelineRepository.findById(1L)).thenReturn(Optional.of(pipeline1));

    Optional<Pipeline> result = pipelineRepository.findById(1L);

    verify(pipelineRepository, times(1)).findById(1L);
    assertTrue(result.isPresent());
    assertEquals(pipeline1, result.get());
  }

  @Test
  void testFindByStatus() {
    when(pipelineRepository.findByStatus("SUCCESS")).thenReturn(List.of(pipeline1));

    List<Pipeline> result = pipelineRepository.findByStatus("SUCCESS");

    verify(pipelineRepository, times(1)).findByStatus("SUCCESS");
    assertEquals(1, result.size());
    assertEquals(pipeline1, result.get(0));
  }

  @Test
  void testFindByStartTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T09:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T11:00:00Z"));

    when(pipelineRepository.findByStartTimeBetween(start, end)).thenReturn(List.of(pipeline1));

    List<Pipeline> result = pipelineRepository.findByStartTimeBetween(start, end);

    verify(pipelineRepository, times(1)).findByStartTimeBetween(start, end);
    assertEquals(1, result.size());
    assertEquals(pipeline1, result.get(0));
  }

  @Test
  void testFindByEndTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T12:00:00Z"));

    when(pipelineRepository.findByEndTimeBetween(start, end)).thenReturn(List.of(pipeline1));

    List<Pipeline> result = pipelineRepository.findByEndTimeBetween(start, end);

    verify(pipelineRepository, times(1)).findByEndTimeBetween(start, end);
    assertEquals(1, result.size());
    assertEquals(pipeline1, result.get(0));
  }

  @Test
  void testFindByRepoUrlAndStartTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T09:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T11:00:00Z"));

    when(pipelineRepository.findByRepoUrlAndStartTimeBetween("https://github.com/wp161/cicd-localrepo.git", start, end))
      .thenReturn(List.of(pipeline1));

    List<Pipeline> result = pipelineRepository
      .findByRepoUrlAndStartTimeBetween("https://github.com/wp161/cicd-localrepo.git", start, end);

    verify(pipelineRepository, times(1))
      .findByRepoUrlAndStartTimeBetween("https://github.com/wp161/cicd-localrepo.git", start, end);
    assertEquals(1, result.size());
    assertEquals(pipeline1, result.get(0));
  }

  @Test
  void testFindByRepoUrlAndStatus() {
    when(pipelineRepository.findByRepoUrlAndStatus("https://github.com/wp161/cicd-localrepo.git", "SUCCESS"))
      .thenReturn(List.of(pipeline1));

    List<Pipeline> result = pipelineRepository
      .findByRepoUrlAndStatus("https://github.com/wp161/cicd-localrepo.git", "SUCCESS");

    verify(pipelineRepository, times(1))
      .findByRepoUrlAndStatus("https://github.com/wp161/cicd-localrepo.git", "SUCCESS");
    assertEquals(1, result.size());
    assertEquals(pipeline1, result.get(0));
  }

  @Test
  void testFindTopByOrderByStartTimeDesc() {
    when(pipelineRepository.findTopByOrderByStartTimeDesc()).thenReturn(Optional.of(pipeline2));

    Optional<Pipeline> result = pipelineRepository.findTopByOrderByStartTimeDesc();

    verify(pipelineRepository, times(1)).findTopByOrderByStartTimeDesc();
    assertTrue(result.isPresent());
    assertEquals(pipeline2, result.get());
  }
}
