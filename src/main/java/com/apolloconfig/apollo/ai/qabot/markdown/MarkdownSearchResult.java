package com.apolloconfig.apollo.ai.qabot.markdown;

public class MarkdownSearchResult {

  private final String fileRoot;
  private final String content;

  public MarkdownSearchResult(String fileRoot, String content) {
    this.fileRoot = fileRoot;
    this.content = content;
  }

  public String getFileRoot() {
    return fileRoot;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "MarkdownSearchResult{" +
        "fileRoot='" + fileRoot + '\'' +
        ", content='" + content + '\'' +
        '}';
  }
}
