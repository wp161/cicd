package neu.cs6510.shared.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PipelineTest {

  @Test
  void addStage() {
    Pipeline pipeline = new Pipeline();
    Stage stage = new Stage();
    stage.setName("Build");
    pipeline.addStage(stage);

    List<Stage> stages = pipeline.getStages();
    assertEquals(1, stages.size());
    assertEquals("Build", stages.get(0).getName());
  }
}