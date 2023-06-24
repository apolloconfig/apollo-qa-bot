package com.apolloconfig.apollo.ai.qabot.openai;

import com.google.common.collect.Lists;
import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("openai")
@Component
class OpenAiService implements AiService {

  private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
  private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-ada-002";

  private final com.theokanning.openai.service.OpenAiService service;

  public OpenAiService() {
    service = OpenAiServiceFactory.getService(System.getenv("OPENAI_API_KEY"));
  }

  public String getCompletion(String prompt) {
    ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), prompt);
    return getCompletionFromMessages(Lists.newArrayList(message));
  }

  public String getCompletionFromMessages(List<ChatMessage> messages) {
    return getCompletionFromMessages(messages, 0.0);
  }

  public String getCompletionFromMessages(List<ChatMessage> messages, double temperature) {
    return getCompletionFromMessages(messages, DEFAULT_MODEL, temperature, 500);
  }

  public String getCompletionFromMessages(List<ChatMessage> messages, String model,
      double temperature, int maxTokens) {
    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
        .builder()
        .model(model)
        .messages(messages)
        .temperature(temperature)
        .maxTokens(maxTokens)
        .build();

    return service.createChatCompletion(chatCompletionRequest).getChoices().get(0).getMessage()
        .getContent();
  }

  public List<Embedding> getEmbeddings(List<String> chunks) {
    EmbeddingRequest embeddingRequest = EmbeddingRequest.builder().model(DEFAULT_EMBEDDING_MODEL)
        .input(chunks).build();

    return service.createEmbeddings(embeddingRequest).getData();
  }
}
