package com.apolloconfig.apollo.ai.qabot.config;

import com.google.common.collect.Lists;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "markdown.files")
@Component
public class MarkdownFilesConfig {

  private String location;
  private List<String> roots = Lists.newLinkedList();

  public List<String> getRoots() {
    return roots;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setRoots(List<String> roots) {
    this.roots = roots;
  }
}
