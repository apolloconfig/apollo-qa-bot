package com.apolloconfig.apollo.ai.qabot.milvus;

import com.google.common.collect.Maps;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import java.util.Map;
import org.crac.Context;
import org.crac.Resource;

public class MilvusClientFactory implements Resource {

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

  public static MilvusServiceClient getCloudClient(String uri, String token) {
    if (!clients.containsKey(uri)) {
      synchronized (INSTANCE) {
        if (!clients.containsKey(uri)) {
          clients.put(uri, INSTANCE.createCloudClient(uri, token));
        }
      }
    }

    return clients.get(uri);
  }

  private MilvusServiceClient createClient(String host, int port) {
    return new MilvusServiceClient(
        ConnectParam.newBuilder().withHost(host).withPort(port).build());
  }

  private MilvusServiceClient createCloudClient(String uri, String token) {
    return new MilvusServiceClient(ConnectParam.newBuilder().withUri(uri).withToken(token).build());
  }

  @Override
  public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
    clients.clear();
  }

  @Override
  public void afterRestore(Context<? extends Resource> context) throws Exception {

  }
}
