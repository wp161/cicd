package neu.cs6510.shared.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import neu.cs6510.shared.entity.Job;
import neu.cs6510.shared.entity.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StageTest {
  private Stage stage;

  @BeforeEach
  void setUp() {
    stage = new Stage();  // Create a new Stage object before each test
  }

  @Test
  void testAddJob() {
    Job job = Job.builder().build();
    stage.addJob(job);
    assertEquals(1, stage.getJobs().size());
    assertTrue(stage.getJobs().contains(job));
  }

  @Test
  void testHasJob() {
    Job job = Job.builder().name("job").build();
    stage.addJob(job);
    assertTrue(stage.hasJob("job"));
  }
}