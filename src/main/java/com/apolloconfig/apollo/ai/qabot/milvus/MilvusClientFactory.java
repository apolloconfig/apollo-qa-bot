package com.apolloconfig.apollo.ai.qabot.milvus;

import com.google.common.collect.Maps;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import java.util.Map;

public class MilvusClientFactory {

  private static final MilvusClientFactory INSTANCE = new MilvusClientFactory();
  private static final Map<String, MilvusServiceClient> clients = Maps.newConcurrentMap();

  public static MilvusServiceClient getClient(String host, int port) {
    String key = host + ":" + port;
    if (!clients.containsKey(key)) {
      synchronized (INSTANCE) {
        if (!clients.containsKey(key)) {
          clients.put(key, INSTANCE.createClient(host, port));
        }
      }
    }

    return clients.get(key);
  }

  private MilvusServiceClient createClient(String host, int port) {
    return new MilvusServiceClient(
        ConnectParam.newBuilder().withHost(host).withPort(port).build());
  }

}
