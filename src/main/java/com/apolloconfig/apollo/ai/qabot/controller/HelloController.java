package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.google.common.collect.Lists;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

  private final AiService aiService;

  public HelloController(AiService aiService) {
    this.aiService = aiService;
  }

  @GetMapping("/{name}")
  public String hello(@PathVariable String name) {
    ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(),
        "You are an assistant who responds in the style of Dr Seuss.");
    ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(),
        "write a brief greeting for " + name);

    String result = aiService.getCompletionFromMessages(
        Lists.newArrayList(systemMessage, userMessage));

    return result;
  }
}
