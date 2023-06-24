package com.apolloconfig.apollo.ai.qabot.controller;

import com.apolloconfig.apollo.ai.qabot.config.MarkdownFilesConfig;
import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownProcessor;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/markdown")
public class MarkdownController {

  private final MarkdownProcessor markdownProcessor;

  private final MarkdownFilesConfig markdownFilesConfig;

  public MarkdownController(MarkdownProcessor markdownProcessor,
      MarkdownFilesConfig markdownFilesConfig) {
    this.markdownProcessor = markdownProcessor;
    this.markdownFilesConfig = markdownFilesConfig;
  }

  @GetMapping("/load")
  public List<String> loadAndProcessFiles() {
    return markdownProcessor.loadAndProcessFiles(markdownFilesConfig.getLocation());
  }
}
