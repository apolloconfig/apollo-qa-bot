package com.apolloconfig.apollo.ai.qabot.markdown;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.apolloconfig.apollo.ai.qabot.api.VectorDBService;
import com.apolloconfig.apollo.ai.qabot.config.MarkdownFilesConfig;
import com.apolloconfig.apollo.ai.qabot.config.MarkdownProcessorRetryConfig;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import retrofit2.HttpException;

@ExtendWith(MockitoExtension.class)
class MarkdownProcessorTest {

  @Mock
  private MarkdownFilesConfig markDownFilesConfig;
  @Mock
  private MarkdownProcessorRetryConfig markdownProcessorRetryConfig;
  @Mock
  private AiService aiService;
  @Mock
  private VectorDBService vectorDBService;

  private MarkdownProcessor markdownProcessor;

  private Path parentFolder;

  @BeforeEach
  void setUp() throws Exception {
    when(markdownProcessorRetryConfig.getDelay()).thenReturn(1l);
    when(markdownProcessorRetryConfig.getMultiplier()).thenReturn(2.0d);
    when(markdownProcessorRetryConfig.getMaxDelay()).thenReturn(5l);
    when(markdownProcessorRetryConfig.getMaxElapsedTime()).thenReturn(10l);

    markdownProcessor = Mockito.spy(new MarkdownProcessor(markDownFilesConfig, markdownProcessorRetryConfig,
        aiService, vectorDBService));

    parentFolder = Paths.get("test-" + System.currentTimeMillis());
    Files.createDirectory(parentFolder);
  }

  @AfterEach
  void tearDown() throws Exception {
    Files.walk(parentFolder)
        .sorted(java.util.Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @Test
  void testProcessFile() throws Exception {
    String someFile = "someFile.md";
    String anotherFile = "anotherFile.md";

    String location = parentFolder.toAbsolutePath().toString();
    Path someFilePath = Files.createFile(parentFolder.resolve(someFile)).toAbsolutePath();
    Path anotherFilePath = Files.createFile(parentFolder.resolve(anotherFile)).toAbsolutePath();

    doReturn(true).when(markdownProcessor).processFile(someFilePath);
    doReturn(true).when(markdownProcessor).processFile(anotherFilePath);

    List<String> updatedFiles = markdownProcessor.loadAndProcessFiles(location);

    assertEquals(2, updatedFiles.size());
    assertTrue(updatedFiles.contains(someFilePath.toString()));
    assertTrue(updatedFiles.contains(anotherFilePath.toString()));
    verify(markdownProcessor, times(1)).processFile(someFilePath);
  }

  @Test
  void testProcessFileWithRetry() throws Exception {
    String someFile = "someFile.md";
    String location = parentFolder.toAbsolutePath().toString();
    Path someFilePath = Files.createFile(parentFolder.resolve(someFile)).toAbsolutePath();

    HttpException mockException = mock(HttpException.class);
    when(mockException.code()).thenReturn(429);

    doAnswer(new Answer<Boolean>() {
      private int counter = 0;

      @Override
      public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
        if (counter < 2) {
          counter++;
          throw mockException;
        }
        return Boolean.TRUE;
      }
    }).when(markdownProcessor).processFile(someFilePath);

    List<String> updatedFiles = markdownProcessor.loadAndProcessFiles(location);

    assertEquals(1, updatedFiles.size());
    assertTrue(updatedFiles.contains(someFilePath.toString()));
    verify(markdownProcessor, times(3)).processFile(someFilePath);
  }

  @Test
  void testProcessFileWithException() throws Exception {
    String someFile = "someFile.md";
    String location = parentFolder.toAbsolutePath().toString();
    Path someFilePath = Files.createFile(parentFolder.resolve(someFile)).toAbsolutePath();

    HttpException mockException = mock(HttpException.class);
    when(mockException.code()).thenReturn(500);

    doThrow(mockException).when(markdownProcessor).processFile(someFilePath);

    List<String> updatedFiles = markdownProcessor.loadAndProcessFiles(location);

    assertTrue(updatedFiles.isEmpty());
    verify(markdownProcessor, times(1)).processFile(someFilePath);
  }
}