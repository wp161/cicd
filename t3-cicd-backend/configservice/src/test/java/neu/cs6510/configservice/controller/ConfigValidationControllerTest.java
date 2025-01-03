package neu.cs6510.configservice.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import neu.cs6510.configservice.service.ConfigFileService;
import neu.cs6510.configservice.service.ValidationService;
import neu.cs6510.shared.entity.Pipeline;
import neu.cs6510.shared.repository.PipelineRepository;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConfigValidationController.class)
@ContextConfiguration(classes = {ConfigValidationController.class})
class ConfigValidationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ValidationService validationService;

  @MockBean
  private PipelineRepository pipelineRepository;

  @MockBean
  private ConfigFileService configFileService;

  private File tempFile;

  @BeforeEach
  void setUp() throws IOException, GitAPIException {
    tempFile = File.createTempFile("test", ".yml");
    tempFile.deleteOnExit(); // Ensure the file is deleted after the test run

    try (FileWriter writer = new FileWriter(tempFile)) {
      writer.write("stages:\n  - build\n");
    }

    when(configFileService.cloneRepoToPv("https://github.com/example/repo.git", "main"))
      .thenReturn("/path/to/cloned/repo");
    when(configFileService.findConfigFile("/path/to/cloned/repo", "path/to/config.yml", null))
      .thenReturn(tempFile);
  }

  @Test
  void testUploadYamlSuccess() throws Exception {
    when(validationService.parseAndValidateConfigFile(tempFile, "https://github.com/example/repo.git")).thenReturn(
      Pipeline.builder().id(12345L).build());

    String requestBody = """
            {
              "repo_url": "https://github.com/example/repo.git",
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isOk());
  }

  @Test
  void testUploadYamlFailureParsing() throws Exception {
    when(validationService.parseAndValidateConfigFile(tempFile, "https://github.com/example/repo.git")).thenAnswer(invocation -> {
      throw new RuntimeException("Invalid YAML file");
    });

    String requestBody = """
            {
              "repo_url": "https://github.com/example/repo.git",
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
      .contentType(MediaType.APPLICATION_JSON)
      .content(requestBody))
      .andExpect(status().isBadRequest());
  }

  @Test
  void testUploadYamlFailureIllegalArgument() throws Exception {
    when(configFileService.cloneRepoToPv("https://github.com/example/repo.git", "main"))
      .thenThrow(new IllegalArgumentException("Missing required parameter"));

    String requestBody = """
            {
              "repo_url": "https://github.com/example/repo.git",
              "branch": "main"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("One of configPath or pipelineName must be provided."));
      });
  }

  @Test
  void testUploadYamlFailureGitAPIException() throws Exception {
    when(configFileService.cloneRepoToPv("https://github.com/example/repo.git", "main"))
      .thenThrow(new GitAPIException("Failed to clone repository") {});

    String requestBody = """
            {
              "repo_url": "https://github.com/example/repo.git",
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Failed to clone repository"));
      });
  }

  @Test
  void testUploadYamlFailureFileNotFoundException() throws Exception {
    when(configFileService.findConfigFile("/path/to/cloned/repo", "path/to/config.yml", null))
      .thenThrow(new IOException("Config file not found"));

    String requestBody = """
            {
              "repo_url": "https://github.com/example/repo.git",
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Failed to find configuration file"));
      });
  }

  @Test
  void testUploadYamlFailureValidationIOException() throws Exception {
    when(validationService.parseAndValidateConfigFile(tempFile, "https://github.com/example/repo.git"))
      .thenThrow(new IOException("Error while reading YAML file"));

    String requestBody = """
            {
              "repo_url": "https://github.com/example/repo.git",
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Validation failed: Error while reading YAML file"));
      });
  }

  @Test
  void testUploadYamlFailureEmptyParams() throws Exception {
    String requestBody = """
            {
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request parameter error: Request is empty"));
      });
  }

  @Test
  void testUploadYamlFailureNoRepoUrl() throws Exception {
    String requestBody = """
            {
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request must contain a non-empty string for repo URL"));
      });

     requestBody = """
            { "repo_url": "",
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request must contain a non-empty string for repo URL"));
      });

    requestBody = """
            { "repo_url": 12345,
              "branch": "main",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request must contain a non-empty string for repo URL"));
      });
  }

  @Test
  void testUploadYamlFailureNoBranch() throws Exception {
    String requestBody = """
            {"repo_url": "https://github.com/example/repo.git",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request must contain a non-empty string for branch name"));
      });

    requestBody = """
            { "repo_url": "https://github.com/example/repo.git",
              "branch": "",
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request must contain a non-empty string for branch name"));
      });

    requestBody = """
            { "repo_url": "https://github.com/example/repo.git",
              "branch": 12345,
              "config_path": "path/to/config.yml"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Request must contain a non-empty string for branch name"));
      });
  }

  @Test
  void testUploadYamlFailureNoConfigPathAndPipeline() throws Exception {
    String requestBody = """
            { "repo_url": "https://github.com/example/repo.git",
              "branch": "main"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("One of configPath or pipelineName must be provided."));
      });
  }

  @Test
  void testUploadYamlFailureBothConfigPathAndPipeline() throws Exception {
    String requestBody = """
            { "repo_url": "https://github.com/example/repo.git",
              "branch": "main",
              "config_path": "path/to/config.yml",
              "pipeline_name": "test"
            }
            """;

    mockMvc.perform(post("/validate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestBody))
      .andExpect(status().isBadRequest())
      .andExpect(result -> {
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("Only one of configPath or pipelineName should be provided, not both."));
      });
  }
}