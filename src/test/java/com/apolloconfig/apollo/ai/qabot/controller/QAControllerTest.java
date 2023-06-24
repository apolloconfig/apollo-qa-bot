package com.apolloconfig.apollo.ai.qabot.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.apolloconfig.apollo.ai.qabot.api.VectorDBService;
import com.apolloconfig.apollo.ai.qabot.controller.QAController.Answer;
import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownSearchResult;
import com.theokanning.openai.embedding.Embedding;
import java.util.Collections;
import java.util.List;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QAControllerTest {

  @Mock
  private AiService aiService;
  @Mock
  private VectorDBService vectorDBService;

  @InjectMocks
  private QAController qaController;

  private String somePrompt;

  private int someTopK;

  private String someQuestion;

  private List<Float> originalEmbeddingValues;

  private Embedding someEmbedding;

  @BeforeEach
  void setUp() {
    somePrompt = "somePrompt";
    someTopK = 2;
    someQuestion = "someQuestion";
    originalEmbeddingValues = Lists.newArrayList(1.0f, 2.0f);
    List<Double> embeddingValues = originalEmbeddingValues.stream().map(Double::valueOf)
        .collect(Lists::newArrayList, List::add, List::addAll);
    someEmbedding = mock(Embedding.class);

    lenient().when(someEmbedding.getEmbedding()).thenReturn(embeddingValues);

    ReflectionTestUtils.setField(qaController, "prompt", somePrompt);
    ReflectionTestUtils.setField(qaController, "topK", someTopK);
  }

  @Test
  void testQAWithEmptyQuestion() {
    someQuestion = " ";

    Answer answer = qaController.qa(someQuestion);

    assertSame(Answer.EMPTY, answer);
    verify(aiService, never()).getEmbeddings(anyList());
    verify(vectorDBService, never()).search(anyList(), anyInt());
    verify(aiService, never()).getCompletion(anyString());
  }

  @Test
  void testQAWithError() {
    List<String> questionList = Lists.newArrayList(someQuestion);

    when(aiService.getEmbeddings(questionList)).thenThrow(new RuntimeException("some exception"));

    Answer answer = qaController.qa(someQuestion);

    assertSame(Answer.ERROR, answer);
    verify(aiService, times(1)).getEmbeddings(questionList);
  }

  @Test
  void testQAWithUnknownResult() {
    List<String> questionList = Lists.newArrayList(someQuestion);
    List<Embedding> someEmbeddings = Lists.newArrayList(someEmbedding);

    when(aiService.getEmbeddings(questionList)).thenReturn(someEmbeddings);
    when(vectorDBService.search(anyList(), anyInt())).thenReturn(Lists.newArrayList());

    Answer answer = qaController.qa(someQuestion);

    assertSame(Answer.UNKNOWN, answer);
    verify(aiService, times(1)).getEmbeddings(questionList);
    ArgumentCaptor<List<List<Float>>> embeddingValuesCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<Integer> topKCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(vectorDBService, times(1)).search(embeddingValuesCaptor.capture(), topKCaptor.capture());

    List<List<Float>> capturedEmbeddingValues = embeddingValuesCaptor.getValue();
    Integer capturedTopK = topKCaptor.getValue();
    assertEquals(originalEmbeddingValues, capturedEmbeddingValues.get(0));
    assertEquals(someTopK, capturedTopK);
  }

  @Test
  void testQA() {
    String someFileRoot = "someFileRoot";
    String someContent = "someContent";
    String anotherFileRoot = "anotherFileRoot";
    String anotherContent = "anotherContent";
    MarkdownSearchResult someMarkdownSearchResult = new MarkdownSearchResult(someFileRoot,
        someContent);
    MarkdownSearchResult anotherMarkdownSearchResult = new MarkdownSearchResult(anotherFileRoot,
        anotherContent);
    List<String> questionList = Lists.newArrayList(someQuestion);
    List<Embedding> someEmbeddings = Lists.newArrayList(someEmbedding);
    List<List<Float>> searchVectors = Collections.singletonList(originalEmbeddingValues);
    String someAnswer = "someAnswer";

    when(aiService.getEmbeddings(questionList)).thenReturn(someEmbeddings);
    when(vectorDBService.search(searchVectors, someTopK)).thenReturn(
        Lists.newArrayList(someMarkdownSearchResult, anotherMarkdownSearchResult));
    when(aiService.getCompletion(somePrompt)).thenReturn(someAnswer);

    Answer answer = qaController.qa(someQuestion);

    assertEquals(someAnswer, answer.getAnswer());
    assertEquals(Sets.newLinkedHashSet(someFileRoot, anotherFileRoot), answer.getRelatedFiles());
  }
}