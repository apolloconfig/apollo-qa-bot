package com.apolloconfig.apollo.ai.qabot.openai;

import com.apolloconfig.apollo.ai.qabot.config.OpenAiAssistantsConfig;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.openai.client.OpenAIClient;
import com.openai.core.http.AsyncStreamResponse;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.FileSearchTool;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseCreateParams.Builder;
import com.openai.models.responses.ResponseStreamEvent;
import com.theokanning.openai.ListSearchParameters;
import com.theokanning.openai.ListSearchParameters.Order;
import com.theokanning.openai.assistants.assistant.VectorStoreFileRequest;
import com.theokanning.openai.assistants.vector_store_file.VectorStoreFile;
import com.theokanning.openai.file.File;
import com.theokanning.openai.service.OpenAiService;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

@Component
public class OpenAiResponseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiResponseService.class);

  private final OpenAiService service;
  private final OpenAIClient client;
  private final String model;
  private final String instructions;
  private final String vectorStoreId;

  public OpenAiResponseService(OpenAiAssistantsConfig config) {
    service = OpenAiServiceFactory.getService(System.getenv("OPENAI_API_KEY"),
        System.getenv("OPENAI_API_BASE_URL"));
    client = OpenAiServiceFactory.getClient(System.getenv("OPENAI_API_KEY"),
        System.getenv("OPENAI_API_BASE_URL"));
    model = config.getModel();
    instructions = config.getInstructions();
    vectorStoreId = config.getVectorStoreId();
  }

  public Flux<ResponseStreamEvent> getResponseMessage(String previousResponseId, String prompt) {
    Builder paramsBuilder = ResponseCreateParams.builder()
        .instructions(instructions)
        .model(model)
        .input(prompt)
        .reasoning(Reasoning.builder().effort(ReasoningEffort.LOW).build())
        .addTool(FileSearchTool.builder().addVectorStoreId(vectorStoreId).build());
    if (!Strings.isNullOrEmpty(previousResponseId)) {
      paramsBuilder.previousResponseId(previousResponseId);
    }
    ResponseCreateParams responseCreateParams = paramsBuilder.build();
    AsyncStreamResponse<ResponseStreamEvent> response = client.async()
        .responses().createStreaming(responseCreateParams);

    return Flux.create(sink -> {
      response.subscribe(new AsyncStreamResponse.Handler<>() {
        @Override
        public void onNext(ResponseStreamEvent event) {
          sink.next(event);
        }

        @Override
        public void onComplete(@NotNull Optional<Throwable> error) {
          if (error.isPresent()) {
            sink.error(error.get());
          } else {
            sink.complete();
          }
        }
      });
    });
  }

  public Map<String, String> getVectorStoreFileIds() {
    int batchSize = 100;
    boolean hasMore = true;
    String lastId = null;
    Map<String, String> fileIdsMap = Maps.newConcurrentMap();
    Set<String> fileIds = Sets.newHashSet();

    while (hasMore) {
      ListSearchParameters searchParameters = new ListSearchParameters();
      searchParameters.setOrder(Order.ASCENDING);
      searchParameters.setLimit(batchSize);
      if (lastId != null) {
        searchParameters.setAfter(lastId);
      }

      List<VectorStoreFile> files = this.service.listVectorStoreFiles(
          this.vectorStoreId, searchParameters).getData();

      if (CollectionUtils.isEmpty(files)) {
        break;
      }

      for (VectorStoreFile file : files) {
        fileIds.add(file.getId());
      }

      int loadedFiles = fileIds.size();
      lastId = files.get(loadedFiles - 1).getId();
      hasMore = files.size() == batchSize;
    }

    List<File> files = this.service.listFiles();

    for (File file : files) {
      if (fileIds.contains(file.getId())) {
        fileIdsMap.put(file.getFilename(), file.getId());
      }
    }

    return fileIdsMap;
  }

  public String createVectorStoreFile(String filename, String content) {
    String purpose = "assistants";

    InputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    File file = this.service.uploadFile(purpose, contentStream, filename);

    VectorStoreFileRequest request = new VectorStoreFileRequest();
    request.setFileId(file.getId());

    this.service.createVectorStoreFile(this.vectorStoreId, request);

    return file.getId();
  }

  public void deleteVectorStoreFile(String fileId) {
    // first delete the vector store file
    this.service.deleteVectorStoreFile(this.vectorStoreId, fileId);

    // then delete the file
    this.service.deleteFile(fileId);
  }

  public String getFileName(String fileId) {
    return this.service.retrieveFile(fileId).getFilename();
  }
}
