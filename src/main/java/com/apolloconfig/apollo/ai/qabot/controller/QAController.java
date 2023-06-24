package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownSearchResult;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.apolloconfig.apollo.ai.qabot.api.VectorDBService;
import com.theokanning.openai.embedding.Embedding;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qa")
public class QAController {

  private static final Logger LOGGER = LoggerFactory.getLogger(QAController.class);

  private final AiService aiService;
  private final VectorDBService vectorDBService;

  @Value("${qa.prompt}")
  private String prompt;

  @Value("${qa.topK}")
  private int topK;

  public QAController(AiService aiService, VectorDBService vectorDBService) {
    this.aiService = aiService;
    this.vectorDBService = vectorDBService;
  }

  @PostMapping
  public Answer qa(@RequestParam String question) {
    question = question.trim();
    if (Strings.isNullOrEmpty(question)) {
      return Answer.EMPTY;
    }

    try {
      return doQA(question);
    } catch (Throwable exception) {
      LOGGER.error("Error while calling OpenAI API", exception);
      return Answer.ERROR;
    }
  }

  private Answer doQA(String question) {
    List<Embedding> embeddings = aiService.getEmbeddings(Lists.newArrayList(question));

    List<List<Float>> searchVectors = Collections.singletonList(
        embeddings.get(0).getEmbedding().stream()
            .map(Double::floatValue).collect(Collectors.toList()));

    List<MarkdownSearchResult> searchResults = vectorDBService.search(searchVectors, topK);

    if (searchResults.isEmpty()) {
      return Answer.UNKNOWN;
    }

    Set<String> relatedFiles = searchResults.stream()
        .map(MarkdownSearchResult::getFileRoot).collect(Collectors.toSet());

    StringBuilder sb = new StringBuilder();
    searchResults.forEach(
        markdownSearchResult -> sb.append(markdownSearchResult.getContent()).append("\n"));

    String promptMessage = prompt.replace("{question}", question)
        .replace("{context}", sb.toString());

    String answer = aiService.getCompletion(promptMessage);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\nPrompt message: {}\nAnswer: {}", promptMessage, answer);
    }

    return new Answer(answer, relatedFiles);
  }

  static class Answer {

    static final Answer EMPTY = new Answer("", Collections.emptySet());
    static final Answer UNKNOWN = new Answer("Sorry, I don't know the answer.",
        Collections.emptySet());

    static final Answer ERROR = new Answer(
        "Sorry, I can't answer your question right now. Please try again later.",
        Collections.emptySet());

    private final String answer;
    private final Set<String> relatedFiles;

    public Answer(String answer, Set<String> relatedFiles) {
      this.answer = answer;
      this.relatedFiles = relatedFiles;
    }

    public String getAnswer() {
      return answer;
    }

    public Set<String> getRelatedFiles() {
      return relatedFiles;
    }
  }
}
