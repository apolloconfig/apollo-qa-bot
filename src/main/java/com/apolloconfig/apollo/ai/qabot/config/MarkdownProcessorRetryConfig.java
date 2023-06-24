package com.apolloconfig.apollo.ai.qabot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "markdown.processor.retry")
@Component
public class MarkdownProcessorRetryConfig {

  private long delay;
  private double multiplier;
  private long maxDelay;
  private long maxElapsedTime;

  public long getDelay() {
    return delay;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public double getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(double multiplier) {
    this.multiplier = multiplier;
  }

  public long getMaxDelay() {
    return maxDelay;
  }

  public void setMaxDelay(long maxDelay) {
    this.maxDelay = maxDelay;
  }

  public long getMaxElapsedTime() {
    return maxElapsedTime;
  }

  public void setMaxElapsedTime(long maxElapsedTime) {
    this.maxElapsedTime = maxElapsedTime;
  }
}
