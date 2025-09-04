package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.entity.Answer;
import com.apolloconfig.apollo.ai.qabot.openai.OpenAiResponseService;
import com.google.common.base.Strings;
import com.openai.models.responses.ResponseCompletedEvent;
import com.openai.models.responses.ResponseStreamEvent;
import com.openai.models.responses.ResponseTextDeltaEvent;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/qa")
public class QAWithResponseController {

  private static final Logger LOGGER = LoggerFactory.getLogger(QAWithResponseController.class);
  private static final String END_SYMBOL = "$END$";

  private final OpenAiResponseService aiService;

  public QAWithResponseController(OpenAiResponseService aiService) {
    this.aiService = aiService;
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Answer> qa(@RequestParam String question,
      @RequestParam(name = "threadId", required = false, defaultValue = "") String conversationId) {
    question = question.trim();
    if (Strings.isNullOrEmpty(question)) {
      return Flux.just(Answer.EMPTY);
    }

    if (Strings.isNullOrEmpty(conversationId)) {
      conversationId = getConversationId();
    }

    try {
      return doQA(conversationId, question);
    } catch (Throwable exception) {
      LOGGER.error("Error while calling Assistants API", exception);
      return Flux.just(Answer.ERROR);
    }
  }

  private String getConversationId() {
    return aiService.createConversation().id();
  }

  private Flux<Answer> doQA(String conversationId, String question) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("\nPrompt message: {}", question);
    }

    Flux<ResponseStreamEvent> result = aiService.getResponseMessage(conversationId, question);

    return result.filter(
            responseStreamEvent -> responseStreamEvent.isOutputTextDelta()
                || responseStreamEvent.isCompleted())
        .map(responseStreamEvent -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("responseStreamEvent: {}", responseStreamEvent);
          }
          if (responseStreamEvent.isOutputTextDelta() && responseStreamEvent.outputTextDelta()
              .isPresent()) {
            return getAnswerFromOutputTextDelta(responseStreamEvent.outputTextDelta().get());
          } else if (responseStreamEvent.isCompleted() && responseStreamEvent.completed()
              .isPresent()) {
            return getAnswerFromCompleted(conversationId, responseStreamEvent.completed().get());
          }
          return Answer.EMPTY;
        }).onErrorReturn(Answer.ERROR);
  }

  private @NotNull Answer getAnswerFromCompleted(String conversationId, ResponseCompletedEvent responseCompletedEvent) {
    Set<String> relatedFiles = responseCompletedEvent.response().output().stream()
        .filter(responseOutputItem -> responseOutputItem.isMessage()
            && responseOutputItem.message().isPresent())
        .flatMap(responseOutputItem -> responseOutputItem.message().stream())
        .flatMap(message -> message.content().stream())
        .filter(content -> content.isOutputText() && content.outputText().isPresent())
        .flatMap(content -> content.outputText().stream())
        .flatMap(outputText -> outputText.annotations().stream())
        .filter(annotation ->
            annotation.isFileCitation() && annotation.fileCitation().isPresent())
        .flatMap(annotation -> annotation.fileCitation().stream())
        .map(fileCitation -> {
          String fileName = fileCitation.filename();
          if (fileName.endsWith(".md")) {
            fileName = fileName.substring(0, fileName.length() - 3);
          }
          return fileName;
        })
        .collect(Collectors.toSet());
    return new Answer(END_SYMBOL, conversationId, relatedFiles);
  }

  private @NotNull Answer getAnswerFromOutputTextDelta(
      ResponseTextDeltaEvent responseTextDeltaEvent) {
    String text = responseTextDeltaEvent.delta();
    return new Answer(text, "", Collections.emptySet());
  }
}
