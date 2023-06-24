package com.apolloconfig.apollo.ai.qabot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("milvus")
@ConfigurationProperties(prefix = "milvus")
@Component
public class MilvusConfig {

  private String host;
  private int port;
  private String collection;

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getCollection() {
    return collection;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void setCollection(String collection) {
    this.collection = collection;
  }
}
