package com.apolloconfig.apollo.ai.qabot.markdown;

import com.apolloconfig.apollo.ai.qabot.api.AiService;
import com.apolloconfig.apollo.ai.qabot.api.VectorDBService;
import com.apolloconfig.apollo.ai.qabot.config.MarkdownFilesConfig;
import com.apolloconfig.apollo.ai.qabot.config.MarkdownProcessorRetryConfig;
import com.google.common.collect.Maps;
import com.theokanning.openai.embedding.Embedding;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
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

  private final MarkdownFilesConfig markdownFilesConfig;
  private final MarkdownProcessorRetryConfig markdownProcessorRetryConfig;
  private final AiService aiService;
  private final VectorDBService vectorDBService;
  private final BackOff backOff;

  public MarkdownProcessor(MarkdownFilesConfig markdownFilesConfig,
      MarkdownProcessorRetryConfig markdownProcessorRetryConfig, AiService aiService,
      VectorDBService vectorDBService) {
    this.markdownFilesConfig = markdownFilesConfig;
    this.markdownProcessorRetryConfig = markdownProcessorRetryConfig;
    this.aiService = aiService;
    this.vectorDBService = vectorDBService;
    this.backOff = initializeBackOff();
  }

  private BackOff initializeBackOff() {
    ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
    exponentialBackOff.setInitialInterval(markdownProcessorRetryConfig.getDelay());
    exponentialBackOff.setMultiplier(markdownProcessorRetryConfig.getMultiplier());
    exponentialBackOff.setMaxInterval(markdownProcessorRetryConfig.getMaxDelay());
    exponentialBackOff.setMaxElapsedTime(markdownProcessorRetryConfig.getMaxElapsedTime());

    return exponentialBackOff;
  }

  public List<String> loadAndProcessFiles(String location) {
    List<String> updatedFiles = new ArrayList<>();
    Path mdDirectory = Paths.get(location);
    try (Stream<Path> paths = Files.walk(mdDirectory)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".md"))
          .forEach(mdFile -> {
            try {
              boolean result = processFileWithRetry(mdFile);
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

  private boolean processFileWithRetry(Path mdFile) throws IOException {
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
      }
    }

    return false;
  }

  boolean processFile(Path mdFile) throws IOException {
    String fileRoot = getMarkdownFileRoots(mdFile);

    String markdownContent = Files.readString(mdFile);
    String hashValue = computeHash(markdownContent);

    String fileHashValue = vectorDBService.queryFileHashValue(fileRoot);

    if (Objects.equals(hashValue, fileHashValue)) {
      return false;
    }

    LOGGER.debug("File {} has changed", mdFile.getFileName());
    List<String> chunks = splitMarkdownIntoChunks(markdownContent);
    LOGGER.debug("File {} has {} chunks", mdFile.getFileName(), chunks.size());

    // calculate chunks embeddings and store them in the database
    List<Embedding> embeddings = aiService.getEmbeddings(chunks);

    vectorDBService.persistChunkEmbeddings(fileRoot, chunks, embeddings);

    vectorDBService.persistFile(fileRoot, hashValue);

    return true;
  }

  private String getMarkdownFileRoots(Path mdFile) {
    String fullPath = mdFile.toAbsolutePath().toString();
    for (String root : markdownFilesConfig.getRoots()) {
      if (fullPath.contains(root)) {
        fullPath = fullPath.substring(fullPath.indexOf(root));
      }
    }
    if (fullPath.endsWith(".md")) {
      fullPath = fullPath.substring(0, fullPath.length() - 3);
    }

    return fullPath;
  }

  private List<String> splitMarkdownIntoChunks(String markdownContent) {
    List<String> chunks = new ArrayList<>();
    Parser parser = Parser.builder().build();
    Document document = parser.parse(markdownContent);

    boolean lastHeadingHasContent = false;
    StringBuilder chunkContent = new StringBuilder();
    for (Node node : document.getChildren()) {
      if (node instanceof Heading) {
        // Encountered a new heading - treat as start of a new chunk
        if (lastHeadingHasContent) {
          chunks.add(chunkContent.toString());
          chunkContent.setLength(0);
          lastHeadingHasContent = false;
        }
        chunkContent.append(((Heading) node).getText()).append("\n");
      } else {
        lastHeadingHasContent = true;
        chunkContent.append(node.getChars()).append("\n");
      }
    }
    // Don't forget to add the last chunk
    if (chunkContent.length() > 0) {
      chunks.add(chunkContent.toString());
    }

    return chunks;
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
