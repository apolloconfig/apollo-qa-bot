package com.apolloconfig.apollo.ai.qabot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties
@EnableScheduling
@SpringBootApplication
public class QABotApplication {

  public static void main(String[] args) {
    SpringApplication.run(QABotApplication.class, args);
  }

}
