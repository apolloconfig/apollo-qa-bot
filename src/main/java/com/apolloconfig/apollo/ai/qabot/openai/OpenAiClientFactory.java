package com.apolloconfig.apollo.ai.qabot.openai;

import com.openai.OpenAI;
import com.openai.core.OpenAIClient;
import com.openai.core.http.OpenAIHttpClient;
import com.openai.core.http.OkHttpClientAdapter;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;

/**
 * Factory for creating {@link OpenAI} clients. The factory supports using the
 * {@code HTTP_PROXY} environment variable for routing traffic through an HTTP
 * proxy. The implementation is intentionally lightweight compared to the
 * previous third‑party SDK usage.
 */
public final class OpenAiClientFactory {

  private OpenAiClientFactory() {
  }

  public static OpenAI create(String apiKey) {
    String httpProxy = System.getenv("HTTP_PROXY");
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    if (httpProxy != null && !httpProxy.isEmpty()) {
      try {
        URL proxyUrl = new URL(httpProxy);
        Proxy proxy = new Proxy(Proxy.Type.HTTP,
            new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));
        builder.proxy(proxy);
        if (proxyUrl.getUserInfo() != null) {
          String[] userInfo = proxyUrl.getUserInfo().split(":");
          Authenticator proxyAuthenticator = (route, response) -> response.request().newBuilder()
              .header("Proxy-Authorization", Credentials.basic(userInfo[0], userInfo[1]))
              .build();
          builder.proxyAuthenticator(proxyAuthenticator);
        }
      } catch (Exception ignored) {
        // If proxy configuration fails, fall back to default client.
      }
    }

    OpenAIHttpClient httpClient = new OkHttpClientAdapter(builder.build());
    return OpenAI.builder()
        .apiKey(apiKey)
        .httpClient(httpClient)
        .build();
  }
}
