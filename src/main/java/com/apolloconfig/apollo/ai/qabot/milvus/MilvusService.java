package com.apolloconfig.apollo.ai.qabot.milvus;

import com.google.common.collect.Lists;
import com.apolloconfig.apollo.ai.qabot.api.VectorDBService;
import com.apolloconfig.apollo.ai.qabot.config.MilvusConfig;
import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownSearchResult;
import com.theokanning.openai.embedding.Embedding;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.R.Status;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.InsertParam.Field;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Profile("milvus")
@Service
class MilvusService implements VectorDBService {

  private final MilvusServiceClient milvusServiceClient;
  private final MilvusConfig milvusConfig;

  public MilvusService(MilvusConfig milvusConfig) {
    this.milvusConfig = milvusConfig;
    this.milvusServiceClient = MilvusClientFactory.getClient(milvusConfig.getHost(),
        milvusConfig.getPort());
    this.ensureCollections();
  }

  public void persistChunkEmbeddings(String fileRoot, List<String> chunks,
      List<Embedding> embeddings) {
    List<Long> currentChunkIds = queryChunkIdByFileRoot(fileRoot);

    List<String> fileRoots = Lists.newArrayListWithCapacity(chunks.size());
    List<List<Float>> embeddingsList = Lists.newArrayListWithCapacity(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      fileRoots.add(fileRoot);
      embeddingsList.add(embeddings.get(i).getEmbedding().stream().map(Double::floatValue)
          .collect(Collectors.toList()));
    }

    List<Field> fields = new ArrayList<>();
    fields.add(new InsertParam.Field("chunk_content", chunks));
    fields.add(new InsertParam.Field("chunk_embedding", embeddingsList));
    fields.add(new InsertParam.Field("file_root", fileRoots));

    InsertParam insertParam = InsertParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .withFields(fields)
        .build();
    milvusServiceClient.insert(insertParam);

    deleteByChunkIdList(currentChunkIds);

    FlushParam flushParam = FlushParam.newBuilder()
        .withCollectionNames(Lists.newArrayList(milvusConfig.getCollection()))
        .build();
    milvusServiceClient.flush(flushParam);
  }

  public List<MarkdownSearchResult> search(List<List<Float>> searchVectors, int topK) {
    LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .build();

    R<RpcStatus> loadStatus = milvusServiceClient.loadCollection(
        loadCollectionParam);

    List<String> searchOutputFields = Arrays.asList("chunk_id", "chunk_content", "file_root");

    SearchParam searchParam = SearchParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
        .withMetricType(MetricType.L2)
        .withOutFields(searchOutputFields)
        .withTopK(topK)
        .withVectors(searchVectors)
        .withVectorFieldName("chunk_embedding")
        .build();
    R<SearchResults> respSearch = milvusServiceClient.search(searchParam);

    if (respSearch.getStatus() != Status.Success.getCode()) {
      throw new RuntimeException("Search failed: " + respSearch.getMessage());
    }

    if (respSearch.getData().getResults().getScoresCount() == 0) {
      return Collections.emptyList();
    }

    SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(
        respSearch.getData().getResults());

    List<?> chunkContents = wrapperSearch.getFieldData("chunk_content", 0);
    List<?> fileRoots = wrapperSearch.getFieldData("file_root", 0);

    List<MarkdownSearchResult> results = Lists.newArrayListWithCapacity(chunkContents.size());
    for (int i = 0; i < chunkContents.size(); i++) {
      MarkdownSearchResult result = new MarkdownSearchResult((String) fileRoots.get(i),
          (String) chunkContents.get(i));
      results.add(result);
    }

    return results;
  }

  private void deleteByChunkIdList(List<Long> chunkIds) {
    if (!chunkIds.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("chunk_id in [");
      for (int i = 0; i < chunkIds.size(); i++) {
        sb.append(chunkIds.get(i));
        if (i != chunkIds.size() - 1) {
          sb.append(",");
        }
      }
      sb.append("]");
      DeleteParam deleteParam = DeleteParam.newBuilder()
          .withCollectionName(milvusConfig.getCollection())
          .withExpr(sb.toString())
          .build();
      milvusServiceClient.delete(deleteParam);
    }
  }

  private List<Long> queryChunkIdByFileRoot(String fileRoot) {
    LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .build();

    R<RpcStatus> loadStatus = milvusServiceClient.loadCollection(
        loadCollectionParam);

    List<String> query_output_fields = Arrays.asList("chunk_id");
    QueryParam queryParam = QueryParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
        .withExpr(String.format("file_root in ['%s']", fileRoot))
        .withOutFields(query_output_fields)
        .build();
    R<QueryResults> respQuery = milvusServiceClient.query(queryParam);

    QueryResultsWrapper wrapperQuery = new QueryResultsWrapper(respQuery.getData());
    List<?> chunkIds = wrapperQuery.getFieldWrapper("chunk_id").getFieldData();

    if (CollectionUtils.isEmpty(chunkIds)) {
      return Collections.emptyList();
    }

    return chunkIds.stream().map(id -> Long.parseLong(id.toString()))
        .collect(Collectors.toList());
  }

  private void ensureCollections() {
    ensureChunkCollection();
  }

  private void ensureChunkCollection() {
    HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .build();

    if (milvusServiceClient.hasCollection(hasCollectionParam).getData()) {
      return;
    }

    FieldType chunkId = FieldType.newBuilder()
        .withName("chunk_id")
        .withDataType(DataType.Int64)
        .withPrimaryKey(true)
        .withAutoID(true)
        .build();
    FieldType fileRoot = FieldType.newBuilder()
        .withName("file_root")
        .withDataType(DataType.VarChar)
        .withMaxLength(100)
        .build();
    FieldType chunkContent = FieldType.newBuilder()
        .withName("chunk_content")
        .withDataType(DataType.VarChar)
        .withMaxLength(3000)
        .build();
    FieldType chunkEmbedding = FieldType.newBuilder()
        .withName("chunk_embedding")
        .withDataType(DataType.FloatVector)
        .withDimension(1536)
        .build();
    CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
        .withCollectionName(milvusConfig.getCollection())
        .withDescription("QA Search")
        .addFieldType(chunkId)
        .addFieldType(chunkContent)
        .addFieldType(chunkEmbedding)
        .addFieldType(fileRoot)
        .build();

    milvusServiceClient.createCollection(createCollectionReq);

    milvusServiceClient.createIndex(
        CreateIndexParam.newBuilder()
            .withCollectionName(milvusConfig.getCollection())
            .withFieldName("chunk_embedding")
            .withIndexType(IndexType.FLAT)
            .withMetricType(MetricType.L2)
            .withSyncMode(Boolean.FALSE)
            .build()
    );

    // TODO create index for file_root

  }


}
