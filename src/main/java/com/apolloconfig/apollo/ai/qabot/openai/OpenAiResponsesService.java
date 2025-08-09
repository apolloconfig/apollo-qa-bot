package com.apolloconfig.apollo.ai.qabot.openai;

import com.apolloconfig.apollo.ai.qabot.config.OpenAiAssistantsConfig;
import com.apolloconfig.apollo.ai.qabot.entity.Answer;
import com.google.common.collect.Maps;
import com.openai.OpenAI;
import com.openai.api.core.HttpException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Service that wraps the official OpenAI Java SDK using the new Responses API.
 * The implementation here is a simplified replacement of the previous
 * Assistants based service.
 */
@Component
public class OpenAiResponsesService {

  private final OpenAI client;
  private final OpenAiAssistantsConfig config;

  public OpenAiResponsesService(OpenAiAssistantsConfig config) {
    this.config = config;
    this.client = OpenAiClientFactory.create(System.getenv("OPENAI_API_KEY"));
  }

  /**
   * Send a prompt to the OpenAI Responses API and return the answer as a Flux.
   */
  public Flux<Answer> getAnswer(String threadId, String prompt) {
    try {
      var response = client.responses().create(r -> r
          .model(config.getModel())
          .input(prompt)
          .instructions(config.getInstructions())
          .attachments(a -> a.fileSearch(fs -> fs.vectorStoreId(config.getVectorStoreId()))));
      String text = response.outputText();
      return Flux.just(new Answer(text, threadId, Collections.emptySet()));
    } catch (HttpException e) {
      return Flux.just(Answer.ERROR);
    }
  }

  /**
   * The Responses API does not manage conversation threads. We generate a
   * client side thread id to keep the interface compatible with the previous
   * implementation.
   */
  public String createThread() {
    return UUID.randomUUID().toString();
  }

  public Map<String, String> getVectorStoreFileIds() {
    Map<String, String> fileIdsMap = Maps.newConcurrentMap();
    var files = client.vectorStores().files().list(config.getVectorStoreId());
    files.data().forEach(file -> fileIdsMap.put(file.filename(), file.id()));
    return fileIdsMap;
  }

  public String createVectorStoreFile(String filename, String content) {
    InputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    var file = client.files().upload(f -> f.purpose("assistants").file(contentStream, filename));
    client.vectorStores().files().create(config.getVectorStoreId(),
        f -> f.fileId(file.id()));
    return file.id();
  }

  public void deleteVectorStoreFile(String fileId) {
    client.vectorStores().files().delete(config.getVectorStoreId(), fileId);
    client.files().delete(fileId);
  }

  public String getFileName(String fileId) {
    return client.files().retrieve(fileId).filename();
  }
}
