package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.apolloconfig.apollo.ai.qabot.api.VectorDBService;
import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownSearchResult;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.embedding.Embedding;
import io.reactivex.Flowable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Answer> qa(@RequestParam String question) {
    question = question.trim();
    if (Strings.isNullOrEmpty(question)) {
      return Flux.just(Answer.EMPTY);
    }

    try {
      return doQA(question);
    } catch (Throwable exception) {
      LOGGER.error("Error while calling OpenAI API", exception);
      return Flux.just(Answer.ERROR);
    }
  }

  /**
   * @deprecated Use {@link #qa(String)} instead.
   */
  @Deprecated
  @PostMapping
  public Mono<Answer> qaSync(ServerWebExchange serverWebExchange) {
    Mono<String> field = getFormField(serverWebExchange, "question");
    return field.flatMap(question -> {
      if (Strings.isNullOrEmpty(question)) {
        return Mono.just(Answer.EMPTY);
      }

      try {
        Flux<Answer> answer = doQA(question.trim());
        return answer.reduce((a1, a2) -> {
          if (Answer.END.answer().equals(a2.answer())) {
            return a1;
          }
          a1.relatedFiles().addAll(a2.relatedFiles);

          return new Answer(a1.answer() + a2.answer(), a1.relatedFiles);
        });
      } catch (Throwable exception) {
        LOGGER.error("Error while calling OpenAI API", exception);
        return Mono.just(Answer.ERROR);
      }
    });
  }

  private Mono<String> getFormField(ServerWebExchange exchange, String fieldName) {
    return exchange.getFormData()
        .flatMap(data -> Mono.justOrEmpty(data.getFirst(fieldName)));
  }

  private Flux<Answer> doQA(String question) {
    List<MarkdownSearchResult> searchResults = searchFromVectorDB(question);

    if (searchResults.isEmpty()) {
      return Flux.just(Answer.UNKNOWN);
    }

    Set<String> relatedFiles = searchResults.stream()
        .map(MarkdownSearchResult::getFileRoot).collect(Collectors.toSet());

    String promptMessage = assemblePromptMessage(searchResults, question);

    Flowable<ChatCompletionChunk> result = aiService.getCompletion(promptMessage);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\nPrompt message: {}", promptMessage);
    }

    final AtomicInteger counter = new AtomicInteger();
    Flux<Answer> flux = Flux.from(result.filter(
        chatCompletionChunk -> chatCompletionChunk.getChoices().get(0).getMessage().getContent()
            != null).map(chatCompletionChunk -> {
      String value = chatCompletionChunk.getChoices().get(0).getMessage().getContent();
      if (LOGGER.isDebugEnabled()) {
        System.out.print(value);
      }

      return counter.incrementAndGet() == 1 ? new Answer(value, relatedFiles)
          : new Answer(value, Collections.emptySet());
    }));

    return flux.concatWith(Flux.just(Answer.END));
  }

  private List<MarkdownSearchResult> searchFromVectorDB(String question) {
    List<Embedding> embeddings = aiService.getEmbeddings(Lists.newArrayList(question));

    List<List<Float>> searchVectors = Collections.singletonList(
        embeddings.get(0).getEmbedding().stream()
            .map(Double::floatValue).collect(Collectors.toList()));

    return vectorDBService.search(searchVectors, topK);
  }

  private String assemblePromptMessage(List<MarkdownSearchResult> searchResults, String question) {
    StringBuilder sb = new StringBuilder();
    searchResults.forEach(
        markdownSearchResult -> sb.append(markdownSearchResult.getContent()).append("\n"));

    return prompt.replace("{question}", question)
        .replace("{context}", sb.toString());
  }

  public record Answer(String answer, Set<String> relatedFiles) {

    static final Answer EMPTY = new Answer("", Collections.emptySet());
    static final Answer UNKNOWN = new Answer("Sorry, I don't know the answer.",
        Collections.emptySet());

    static final Answer ERROR = new Answer(
        "Sorry, I can't answer your question right now. Please try again later.",
        Collections.emptySet());

    static final Answer END = new Answer("$END$", Collections.emptySet());
  }
}
