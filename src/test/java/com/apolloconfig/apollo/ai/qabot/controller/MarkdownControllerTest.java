package com.apolloconfig.apollo.ai.qabot.controller;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.apolloconfig.apollo.ai.qabot.config.MarkdownFilesConfig;
import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownProcessor;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarkdownControllerTest {

  @Mock
  private MarkdownProcessor markdownProcessor;

  @Mock
  private MarkdownFilesConfig markdownFilesConfig;

  @InjectMocks
  private MarkdownController markdownControllerUnderTest;

  private String someLocation = "location";

  @BeforeEach
  void setUp() {
    when(markdownFilesConfig.getLocation()).thenReturn(someLocation);
  }

  @Test
  void testLoadAndProcessFiles() {
    List<String> someResult = Lists.newArrayList("result");
    when(markdownProcessor.loadAndProcessFiles(someLocation)).thenReturn(someResult);

    List<String> actualResult = markdownControllerUnderTest.loadAndProcessFiles();

    assertSame(someResult, actualResult);
    verify(markdownProcessor, times(1)).loadAndProcessFiles(someLocation);
    verify(markdownFilesConfig, times(1)).getLocation();
  }

  @Test
  void testLoadAndProcessFileWithException() {
    RuntimeException someException = new RuntimeException();
    when(markdownProcessor.loadAndProcessFiles(someLocation)).thenThrow(someException);

    RuntimeException exception = null;
    try {
      markdownControllerUnderTest.loadAndProcessFiles();
    } catch (RuntimeException e) {
      exception = e;
    }

    assertSame(someException, exception);
    verify(markdownProcessor, times(1)).loadAndProcessFiles(someLocation);
    verify(markdownFilesConfig, times(1)).getLocation();
  }
}