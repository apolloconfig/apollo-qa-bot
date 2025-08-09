package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.entity.Answer;
import com.google.common.base.Strings;
import com.apolloconfig.apollo.ai.qabot.openai.OpenAiResponsesService;
import java.util.Collections;
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
public class QAWithResponsesController {

  private static final Logger LOGGER = LoggerFactory.getLogger(QAWithResponsesController.class);
  private static final String END_SYMBOL = "$END$";

  private final OpenAiResponsesService aiService;

  public QAWithResponsesController(OpenAiResponsesService aiService) {
    this.aiService = aiService;
  }

  @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Answer> qa(@RequestParam String question,
      @RequestParam(required = false, defaultValue = "") String threadId) {
    question = question.trim();
    if (Strings.isNullOrEmpty(question)) {
      return Flux.just(Answer.EMPTY);
    }

    if (Strings.isNullOrEmpty(threadId)) {
      threadId = getThreadId();
    }

    return aiService.getAnswer(threadId, question)
        .onErrorReturn(Answer.ERROR)
        .concatWith(Flux.just(new Answer(END_SYMBOL, threadId, Collections.emptySet())));
  }

  private String getThreadId() {
    return aiService.createThread();
  }
}
