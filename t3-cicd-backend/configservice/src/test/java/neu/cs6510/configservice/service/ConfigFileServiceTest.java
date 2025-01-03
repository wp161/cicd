package neu.cs6510.configservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

class ConfigFileServiceTest {

  private ConfigFileService configFileService;

  @BeforeEach
  void setUp() {
    configFileService = new ConfigFileService();
  }

  @Test
  void testCloneRepoToPvSuccess() throws GitAPIException {
    String repoUrl = "https://github.com/example/repo.git";
    String branch = "main";

    try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
      CloneCommand cloneCommandMock = mock(CloneCommand.class);
      gitMock.when(Git::cloneRepository).thenReturn(cloneCommandMock);

      when(cloneCommandMock.setURI(repoUrl)).thenReturn(cloneCommandMock);
      when(cloneCommandMock.setBranch(branch)).thenReturn(cloneCommandMock);
      when(cloneCommandMock.setDirectory(any(File.class))).thenReturn(cloneCommandMock);
      when(cloneCommandMock.call()).thenReturn(mock(Git.class));

      String repoDir = configFileService.cloneRepoToPv(repoUrl, branch);

      assertNotNull(repoDir);
      assertTrue(repoDir.startsWith("/mnt/git-repo/repo"));
    }
  }

  @Test
  void testCloneRepoToPvFailure() throws GitAPIException {
    String repoUrl = "https://invalid-repo-url.git";
    String branch = "main";

    try (MockedStatic<Git> gitMock = mockStatic(Git.class)) {
      CloneCommand cloneCommandMock = mock(CloneCommand.class);
      gitMock.when(Git::cloneRepository).thenReturn(cloneCommandMock);

      when(cloneCommandMock.setURI(repoUrl)).thenReturn(cloneCommandMock);
      when(cloneCommandMock.setBranch(branch)).thenReturn(cloneCommandMock);
      when(cloneCommandMock.setDirectory(any(File.class))).thenReturn(cloneCommandMock);
      when(cloneCommandMock.call()).thenThrow(new GitAPIException("Failed to clone repository") {});

      GitAPIException exception = assertThrows(GitAPIException.class, () ->
        configFileService.cloneRepoToPv(repoUrl, branch));
      assertEquals("Failed to clone repository", exception.getMessage());
    }
  }

  @Test
  void testFindConfigFileByConfigPath() throws IOException {
    File tempDir = Files.createTempDirectory("test-repo").toFile();
    File configFile = new File(tempDir, "config.yml");
    try (FileWriter writer = new FileWriter(configFile)) {
      writer.write("stages:\n  - build\n");
    }

    File foundFile = configFileService.findConfigFile(tempDir.getAbsolutePath(),
      "config.yml", null);

    assertNotNull(foundFile);
    assertEquals(configFile.getAbsolutePath(), foundFile.getAbsolutePath());
  }

  @Test
  void testFindConfigFileByPipelineName() throws IOException {
    File tempDir = Files.createTempDirectory("test-repo").toFile();
    File pipelinesDir = new File(tempDir, ".cicd-pipelines");
    if (!pipelinesDir.exists() && !pipelinesDir.mkdirs()) {
      throw new IOException("Failed to create directory: " + pipelinesDir.getAbsolutePath());
    }

    File yamlFile = new File(pipelinesDir, "pipeline.yml");
    try (FileWriter writer = new FileWriter(yamlFile)) {
      writer.write("default:\n  name: my-pipeline\n");
    }

    File foundFile = configFileService.findConfigFile(tempDir.getAbsolutePath(),
      null, "my-pipeline");
    assertNotNull(foundFile, "Expected to find a configuration file, but none was found.");
    assertEquals(yamlFile.getAbsolutePath(), foundFile.getAbsolutePath());
  }

  @Test
  void testFindConfigFileNoMatch() throws IOException {
    File tempDir = Files.createTempDirectory("test-repo").toFile();

    File foundFile = configFileService.findConfigFile(tempDir.getAbsolutePath(), null,
      "non-existent-pipeline");

    assertNull(foundFile);
  }
}
