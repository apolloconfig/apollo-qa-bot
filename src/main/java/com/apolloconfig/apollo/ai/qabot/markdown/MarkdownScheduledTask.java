package com.apolloconfig.apollo.ai.qabot.markdown;

import com.apolloconfig.apollo.ai.qabot.config.MarkdownFilesConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "markdown.files.scheduleEnabled", havingValue = "true")
public class MarkdownScheduledTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(MarkdownScheduledTask.class);

  private final MarkdownProcessor markdownProcessor;

  private final MarkdownFilesConfig markdownFilesConfig;

  public MarkdownScheduledTask(MarkdownProcessor markdownProcessor,
      MarkdownFilesConfig markdownFilesConfig) {
    this.markdownProcessor = markdownProcessor;
    this.markdownFilesConfig = markdownFilesConfig;
  }

  @Scheduled(cron = "${markdown.files.scheduleCron}")
  public void update() throws IOException, InterruptedException {
    LOGGER.debug("Start to update markdown files.");
    this.updateMarkdownFiles();
    LOGGER.debug("Start to load and process markdown files.");
    List<String> updatedFiles = this.markdownProcessor.loadAndProcessFiles(
        markdownFilesConfig.getLocation());
    LOGGER.debug("Updated files: {}", updatedFiles);
  }

  private void updateMarkdownFiles() {
    ProcessBuilder pb = new ProcessBuilder("git", "pull");
    pb.directory(new File(markdownFilesConfig.getLocation()));

    try {
      Process process = pb.start();
      boolean finished = process.waitFor(1, TimeUnit.MINUTES);
      if (finished) {
        int exitCode = process.exitValue();
        if (exitCode == 0) {
          LOGGER.debug("Git pull executed successfully.");
        } else {
          StringBuilder sb = new StringBuilder();
          BufferedReader errorReader = new BufferedReader(
              new InputStreamReader(process.getErrorStream()));
          String line;
          while ((line = errorReader.readLine()) != null) {
            sb.append(line);
          }
          LOGGER.error("Git pull execution failed: {}", sb);
        }
      } else {
        LOGGER.error("Git pull execution timed out.");
      }
    } catch (Exception e) {
      LOGGER.error("Git pull execution failed.", e);
    }
  }
}
