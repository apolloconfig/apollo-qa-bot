package com.apolloconfig.apollo.ai.qabot.openai;

import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.google.common.collect.Lists;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.embedding.Embedding;
import com.theokanning.openai.embedding.EmbeddingRequest;
import io.reactivex.Flowable;
import java.util.List;
import org.crac.Context;
import org.crac.Resource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("openai")
@Component
class OpenAiService implements AiService, Resource {

  private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
  private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-ada-002";

  private com.theokanning.openai.service.OpenAiService service;

  public OpenAiService() {
    init();
  }

  private void init() {
    service = OpenAiServiceFactory.getService(System.getenv("OPENAI_API_KEY"));
  }

  public Flowable<ChatCompletionChunk> getCompletion(String prompt) {
    ChatMessage message = new ChatMessage(ChatMessageRole.USER.value(), prompt);
    return getCompletionFromMessages(Lists.newArrayList(message));
  }

  public Flowable<ChatCompletionChunk> getCompletionFromMessages(List<ChatMessage> messages) {
    return getCompletionFromMessages(messages, 0.0);
  }

  public Flowable<ChatCompletionChunk> getCompletionFromMessages(List<ChatMessage> messages,
      double temperature) {
    return getCompletionFromMessages(messages, DEFAULT_MODEL, temperature, 500);
  }

  public Flowable<ChatCompletionChunk> getCompletionFromMessages(List<ChatMessage> messages,
      String model,
      double temperature, int maxTokens) {
    ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
        .builder()
        .model(model)
        .messages(messages)
        .temperature(temperature)
        .maxTokens(maxTokens)
        .build();

    return service.streamChatCompletion(chatCompletionRequest);
  }

  public List<Embedding> getEmbeddings(List<String> chunks) {
    EmbeddingRequest embeddingRequest = EmbeddingRequest.builder().model(DEFAULT_EMBEDDING_MODEL)
        .input(chunks).build();

    return service.createEmbeddings(embeddingRequest).getData();
  }

  @Override
  public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
    this.service = null;
  }

  @Override
  public void afterRestore(Context<? extends Resource> context) throws Exception {
    this.init();
  }
}
