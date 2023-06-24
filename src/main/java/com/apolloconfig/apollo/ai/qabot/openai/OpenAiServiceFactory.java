package com.apolloconfig.apollo.ai.qabot.openai;

import static com.theokanning.openai.service.OpenAiService.defaultClient;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;
import static com.theokanning.openai.service.OpenAiService.defaultRetrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.service.OpenAiService;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class OpenAiServiceFactory {

  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

  private static final OpenAiServiceFactory INSTANCE = new OpenAiServiceFactory();
  private static final Map<String, OpenAiService> SERVICES = Maps.newConcurrentMap();

  public static OpenAiService getService(String apiKey) {
    if (!SERVICES.containsKey(apiKey)) {
      synchronized (INSTANCE) {
        if (!SERVICES.containsKey(apiKey)) {
          SERVICES.put(apiKey, INSTANCE.createService(apiKey));
        }
      }
    }

    return SERVICES.get(apiKey);
  }

  private OpenAiService createService(String apiKey) {
    ObjectMapper mapper = defaultObjectMapper();
    OkHttpClient client = client(apiKey);
    Retrofit retrofit = defaultRetrofit(client, mapper);
    OpenAiApi api = retrofit.create(OpenAiApi.class);
    return new OpenAiService(api);
  }

  private OkHttpClient client(String apiKey) {
    String httpProxy = System.getenv("HTTP_PROXY");

    if (Strings.isNullOrEmpty(httpProxy)) {
      return defaultClient(apiKey, DEFAULT_TIMEOUT);
    }

    URL proxyUrl;
    try {
      proxyUrl = new URL(httpProxy);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid proxy url", e);
    }

    // 创建代理
    Proxy proxy = new Proxy(Proxy.Type.HTTP,
        new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort()));

    String credential = Credentials.basic(proxyUrl.getUserInfo().split(":")[0],
        proxyUrl.getUserInfo().split(":")[1]);
    // 创建认证器
    Authenticator proxyAuthenticator = (route, response) -> response.request().newBuilder()
        .header("Proxy-Authorization", credential)
        .build();

    return defaultClient(apiKey, DEFAULT_TIMEOUT)
        .newBuilder()
        .proxy(proxy)
        .proxyAuthenticator(proxyAuthenticator)
        .socketFactory(new DelegatingSocketFactory(SSLSocketFactory.getDefault()))
        .build();
  }

  private static class DelegatingSocketFactory extends SocketFactory {

    private final SocketFactory delegate;

    public DelegatingSocketFactory(SocketFactory delegate) {
      this.delegate = delegate;
    }

    @Override
    public Socket createSocket() throws IOException {
      Socket socket = delegate.createSocket();
      return configureSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
      Socket socket = delegate.createSocket(host, port);
      return configureSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localAddress,
        int localPort) throws IOException {
      Socket socket = delegate.createSocket(host, port, localAddress, localPort);
      return configureSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      Socket socket = delegate.createSocket(host, port);
      return configureSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress host, int port, InetAddress localAddress,
        int localPort) throws IOException {
      Socket socket = delegate.createSocket(host, port, localAddress, localPort);
      return configureSocket(socket);
    }

    protected Socket configureSocket(Socket socket) {
      // No-op by default.
      return socket;
    }
  }
}
