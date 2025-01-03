package neu.cs6510.shared.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import neu.cs6510.shared.entity.Job;
import neu.cs6510.shared.entity.Stage;
import neu.cs6510.shared.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class JobRepositoryTest {

  @Mock
  private JobRepository jobRepository;

  private Job job1;
  private Job job2;
  private Stage stage;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    stage = Stage.builder()
      .id(1L)
      .name("build")
      .build();

    job1 = Job.builder()
      .name("compile")
      .stage(stage)
      .status("SUCCESS")
      .registry("docker.io")
      .imageName("openjdk:17")
      .startTime(Timestamp.from(Instant.parse("2024-01-01T10:00:00Z")))
      .completionTime(Timestamp.from(Instant.parse("2024-01-01T10:30:00Z")))
      .allowFailure(false)
      .build();

    job2 = Job.builder()
      .name("test")
      .stage(stage)
      .status("RUNNING")
      .registry("docker.io")
      .imageName("python:3.9")
      .startTime(Timestamp.from(Instant.parse("2024-01-02T11:00:00Z")))
      .completionTime(Timestamp.from(Instant.parse("2024-01-02T11:30:00Z")))
      .allowFailure(true)
      .build();
  }

  @Test
  void testFindByStageId() {
    when(jobRepository.findByStageId(1L)).thenReturn(Arrays.asList(job1, job2));

    List<Job> result = jobRepository.findByStageId(1L);

    verify(jobRepository, times(1)).findByStageId(1L);
    assertEquals(2, result.size());
    assertEquals(job1, result.get(0));
    assertEquals(job2, result.get(1));
  }

  @Test
  void testFindByStatus() {
    when(jobRepository.findByStatus("SUCCESS")).thenReturn(List.of(job1));

    List<Job> result = jobRepository.findByStatus("SUCCESS");

    verify(jobRepository, times(1)).findByStatus("SUCCESS");
    assertEquals(1, result.size());
    assertEquals(job1, result.get(0));
  }

  @Test
  void testFindByStageIdAndStatus() {
    when(jobRepository.findByStageIdAndStatus(1L, "SUCCESS")).thenReturn(List.of(job1));

    List<Job> result = jobRepository.findByStageIdAndStatus(1L, "SUCCESS");

    verify(jobRepository, times(1)).findByStageIdAndStatus(1L, "SUCCESS");
    assertEquals(1, result.size());
    assertEquals(job1, result.get(0));
  }

  @Test
  void testFindByRegistry() {
    when(jobRepository.findByRegistry("docker.io")).thenReturn(Arrays.asList(job1, job2));

    List<Job> result = jobRepository.findByRegistry("docker.io");

    verify(jobRepository, times(1)).findByRegistry("docker.io");
    assertEquals(2, result.size());
    assertEquals(job1, result.get(0));
    assertEquals(job2, result.get(1));
  }

  @Test
  void testFindByImageName() {
    when(jobRepository.findByImageName("openjdk:17")).thenReturn(List.of(job1));

    List<Job> result = jobRepository.findByImageName("openjdk:17");

    verify(jobRepository, times(1)).findByImageName("openjdk:17");
    assertEquals(1, result.size());
    assertEquals(job1, result.get(0));
  }

  @Test
  void testFindByStartTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T09:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T11:00:00Z"));

    when(jobRepository.findByStartTimeBetween(start, end)).thenReturn(List.of(job1));

    List<Job> result = jobRepository.findByStartTimeBetween(start, end);

    verify(jobRepository, times(1)).findByStartTimeBetween(start, end);
    assertEquals(1, result.size());
    assertEquals(job1, result.get(0));
  }

  @Test
  void testFindByCompletionTimeBetween() {
    Timestamp start = Timestamp.from(Instant.parse("2024-01-01T10:00:00Z"));
    Timestamp end = Timestamp.from(Instant.parse("2024-01-01T12:00:00Z"));

    when(jobRepository.findByCompletionTimeBetween(start, end)).thenReturn(List.of(job1));

    List<Job> result = jobRepository.findByCompletionTimeBetween(start, end);

    verify(jobRepository, times(1)).findByCompletionTimeBetween(start, end);
    assertEquals(1, result.size());
    assertEquals(job1, result.get(0));
  }

  @Test
  void testFindByAllowFailure() {
    when(jobRepository.findByAllowFailure(true)).thenReturn(List.of(job2));

    List<Job> result = jobRepository.findByAllowFailure(true);

    verify(jobRepository, times(1)).findByAllowFailure(true);
    assertEquals(1, result.size());
    assertEquals(job2, result.get(0));
  }

  @Test
  void testFindByStageIdAndName() {
    when(jobRepository.findByStageIdAndName(1L, "compile")).thenReturn(job1);

    Job result = jobRepository.findByStageIdAndName(1L, "compile");

    verify(jobRepository, times(1)).findByStageIdAndName(1L, "compile");
    assertEquals(job1, result);
  }

  @Test
  void testFindTopByStageIdOrderByStartTimeDesc() {
    when(jobRepository.findTopByStageIdOrderByStartTimeDesc(1L)).thenReturn(job2);

    Job result = jobRepository.findTopByStageIdOrderByStartTimeDesc(1L);

    verify(jobRepository, times(1)).findTopByStageIdOrderByStartTimeDesc(1L);
    assertEquals(job2, result);
  }

  @Test
  void testFindTopByStageIdOrderByStartTimeAsc() {
    when(jobRepository.findTopByStageIdOrderByStartTimeAsc(1L)).thenReturn(job1);

    Job result = jobRepository.findTopByStageIdOrderByStartTimeAsc(1L);

    verify(jobRepository, times(1)).findTopByStageIdOrderByStartTimeAsc(1L);
    assertEquals(job1, result);
  }

  @Test
  void testFindByStageIdOrderByStartTimeAsc() {
    when(jobRepository.findByStageIdOrderByStartTimeAsc(1L)).thenReturn(Arrays.asList(job1, job2));

    List<Job> result = jobRepository.findByStageIdOrderByStartTimeAsc(1L);

    verify(jobRepository, times(1)).findByStageIdOrderByStartTimeAsc(1L);
    assertEquals(2, result.size());
    assertEquals(job1, result.get(0));
    assertEquals(job2, result.get(1));
  }
}
