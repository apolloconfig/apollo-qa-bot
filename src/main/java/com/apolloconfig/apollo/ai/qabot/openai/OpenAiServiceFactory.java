package com.apolloconfig.apollo.ai.qabot.openai;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.theokanning.openai.service.OpenAiService;
import java.time.Duration;
import java.util.Map;

public class OpenAiServiceFactory {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);
  private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1/";

  private static final OpenAiServiceFactory INSTANCE = new OpenAiServiceFactory();
  private static final Map<String, OpenAiService> SERVICES = Maps.newConcurrentMap();
  private static final Map<String, OpenAIClient> CLIENTS = Maps.newConcurrentMap();

  public static OpenAiService getService(String apiKey, String baseUrl) {
    if (!SERVICES.containsKey(apiKey)) {
      synchronized (INSTANCE) {
        if (!SERVICES.containsKey(apiKey)) {
          SERVICES.put(apiKey, INSTANCE.createService(apiKey, baseUrl));
        }
      }
    }

    return SERVICES.get(apiKey);
  }

  private OpenAiService createService(String apiKey, String baseUrl) {
    if (Strings.isNullOrEmpty(baseUrl)) {
      baseUrl = DEFAULT_BASE_URL;
    }
    return new OpenAiService(apiKey, DEFAULT_TIMEOUT, baseUrl);
  }

  public static OpenAIClient getClient(String apiKey, String baseUrl) {
    if (!CLIENTS.containsKey(apiKey)) {
      synchronized (INSTANCE) {
        if (!CLIENTS.containsKey(apiKey)) {
          CLIENTS.put(apiKey, INSTANCE.createClient(apiKey, baseUrl));
        }
      }
    }

    return CLIENTS.get(apiKey);
  }

  private OpenAIClient createClient(String apiKey, String baseUrl) {
    if (Strings.isNullOrEmpty(baseUrl)) {
      baseUrl = DEFAULT_BASE_URL;
    }
    return OpenAIOkHttpClient.builder().apiKey(apiKey).timeout(DEFAULT_TIMEOUT)
        .baseUrl(baseUrl).build();
  }
}
