package neu.cs6510.pipelineservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = {"neu.cs6510.pipelineservice", "neu.cs6510.shared"})
@EnableJpaRepositories(basePackages = "neu.cs6510.shared.repository")
@EntityScan(basePackages = "neu.cs6510.shared.entity")
public class PipelineRunApplication {

  public static void main(String[] args) {
    SpringApplication.run(PipelineRunApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}