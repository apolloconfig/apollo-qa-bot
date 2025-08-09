package com.apolloconfig.apollo.ai.qabot.markdown;

import com.apolloconfig.apollo.ai.qabot.config.MarkdownFilesConfig;
import com.apolloconfig.apollo.ai.qabot.config.MarkdownProcessorRetryConfig;
import com.apolloconfig.apollo.ai.qabot.openai.OpenAiResponsesService;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;
import retrofit2.HttpException;

@Service
public class MarkdownProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownProcessor.class);

  private final OpenAiResponsesService aiService;
  private final MarkdownFilesConfig markdownFilesConfig;
  private final MarkdownProcessorRetryConfig markdownProcessorRetryConfig;
  private final BackOff backOff;
  private final Map<String, String> fileHashValues;
  private final Map<String, String> fileIds;

  public MarkdownProcessor(OpenAiResponsesService aiService,
      MarkdownFilesConfig markdownFilesConfig,
      MarkdownProcessorRetryConfig markdownProcessorRetryConfig) {
    this.aiService = aiService;
    this.markdownFilesConfig = markdownFilesConfig;
    this.markdownProcessorRetryConfig = markdownProcessorRetryConfig;
    this.backOff = initializeBackOff();
    this.fileHashValues = Maps.newConcurrentMap();
    this.fileIds = this.aiService.getVectorStoreFileIds();
  }

  private BackOff initializeBackOff() {
    ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
    exponentialBackOff.setInitialInterval(markdownProcessorRetryConfig.getDelay());
    exponentialBackOff.setMultiplier(markdownProcessorRetryConfig.getMultiplier());
    exponentialBackOff.setMaxInterval(markdownProcessorRetryConfig.getMaxDelay());
    exponentialBackOff.setMaxElapsedTime(markdownProcessorRetryConfig.getMaxElapsedTime());

    return exponentialBackOff;
  }

  public void initialize(String location) {
    processWithAction(location, path -> {
      try {
        String markdownContent = Files.readString(path);
        fileHashValues.put(getMarkdownFileRoots(path), computeHash(markdownContent));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      return true;
    });
  }

  public List<String> loadAndProcessFiles(String location) {
    return this.processWithAction(location, this::processFileWithRetry);
  }

  private List<String> processWithAction(String location, Function<Path, Boolean> action) {
    List<String> updatedFiles = new ArrayList<>();
    Path mdDirectory = Paths.get(location);
    try (Stream<Path> paths = Files.walk(mdDirectory)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".md"))
          .forEach(mdFile -> {
            try {
              boolean result = action.apply(mdFile);
              if (result) {
                updatedFiles.add(mdFile.toAbsolutePath().toString());
              }
            } catch (Throwable e) {
              LOGGER.error("Error processing file {}", mdFile.getFileName(), e);
            }
          });
    } catch (Throwable e) {
      LOGGER.error("Error reading files from location {}", location, e);
    }

    return updatedFiles;
  }

  private boolean processFileWithRetry(Path mdFile) {
    BackOffExecution backOffExecution = backOff.start();
    while (!Thread.currentThread().isInterrupted()) {
      try {
        return processFile(mdFile);
      } catch (HttpException exception) {
        if (exception.code() == 429) {
          long sleepTime = backOffExecution.nextBackOff();

          if (sleepTime == BackOffExecution.STOP) {
            LOGGER.error("Retry limit exceeded. Stopping");
            break;
          }

          LOGGER.warn("OpenAI API rate limit exceeded. Retrying in {} ms", sleepTime);

          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for retry", e);
            Thread.currentThread().interrupt();
            break;
          }
        } else {
          throw exception;
        }
      } catch (Throwable e) {
        throw new RuntimeException(e);
      }
    }

    return false;
  }

  boolean processFile(Path mdFile) throws IOException {
    String fileRoot = getMarkdownFileRoots(mdFile);

    String markdownContent = Files.readString(mdFile);
    String hashValue = computeHash(markdownContent);

    String fileHashValue = this.fileHashValues.get(fileRoot);

    if (Objects.equals(hashValue, fileHashValue)) {
      return false;
    }

    LOGGER.debug("File {} has changed", mdFile.getFileName());

    String fileId = this.fileIds.get(fileRoot);
    if (fileId != null) {
      this.aiService.deleteVectorStoreFile(fileId);
      this.fileIds.remove(fileRoot);
    }

    String newFileId = this.aiService.createVectorStoreFile(fileRoot, markdownContent);
    this.fileIds.put(fileRoot, newFileId);
    this.fileHashValues.put(fileRoot, hashValue);

    return true;
  }

  private String getMarkdownFileRoots(Path mdFile) {
    String fullPath = mdFile.toAbsolutePath().toString();
    for (String root : markdownFilesConfig.getRoots()) {
      if (fullPath.contains(root)) {
        fullPath = fullPath.substring(fullPath.indexOf(root));
      }
    }

    return fullPath;
  }

  private String computeHash(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(input.getBytes());
      StringBuilder hexString = new StringBuilder(2 * hash.length);
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

}
