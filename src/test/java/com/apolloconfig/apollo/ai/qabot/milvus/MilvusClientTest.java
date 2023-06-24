package com.apolloconfig.apollo.ai.qabot.milvus;

import com.google.common.collect.Lists;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.DataType;
import io.milvus.grpc.GetLoadStateResponse;
import io.milvus.grpc.GetLoadingProgressResponse;
import io.milvus.grpc.QueryResults;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.DescribeCollectionParam;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.GetLoadStateParam;
import io.milvus.param.collection.GetLoadingProgressParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.collection.ReleaseCollectionParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.QueryParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.QueryResultsWrapper;
import io.milvus.response.SearchResultsWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MilvusClientTest {

  private static MilvusServiceClient milvusServiceClient;
  private static final String COLLECTION_NAME = "book";

  @BeforeAll
  static void beforeAll() {
    milvusServiceClient = MilvusClientFactory.getClient("localhost", 19530);
    createCollection();
  }

  @AfterAll
  static void afterAll() {
    dropCollection();
    milvusServiceClient.close();
  }

  @Test
  void testCollection() {
    HasCollectionParam hasCollectionParam = HasCollectionParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();
    System.out.println(
        "has collection book: " + milvusServiceClient.hasCollection(hasCollectionParam));

    DescribeCollectionParam describeCollectionParam = DescribeCollectionParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();
    System.out.println(
        "describe result: " + milvusServiceClient.describeCollection(describeCollectionParam));

    LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();

    R<RpcStatus> loadStatus = milvusServiceClient.loadCollection(
        loadCollectionParam);
    System.out.println("load collection status: " + loadStatus);

    // You can check the loading status
    GetLoadStateParam param = GetLoadStateParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();
    R<GetLoadStateResponse> response = milvusServiceClient.getLoadState(param);
    if (response.getStatus() != R.Status.Success.getCode()) {
      System.out.println(response.getMessage());
    } else {
      System.out.println(response.getData().getState());
    }

    // and loading progress as well
    GetLoadingProgressParam loadingParam = GetLoadingProgressParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();
    R<GetLoadingProgressResponse> loadingResponse = milvusServiceClient.getLoadingProgress(
        loadingParam);
    if (loadingResponse.getStatus() != R.Status.Success.getCode()) {
      System.out.println(loadingResponse.getMessage());
    } else {
      System.out.println(loadingResponse.getData().getProgress());
    }
  }

  @Test
  void testData() {
    Random ran = new Random();
    List<Long> book_id_array = new ArrayList<>();
    List<Long> word_count_array = new ArrayList<>();
    List<List<Float>> book_intro_array = new ArrayList<>();
    for (long i = 0L; i < 2000; ++i) {
      book_id_array.add(i);
      word_count_array.add(i + 10000);
      List<Float> vector = new ArrayList<>();
      for (int k = 0; k < 2; ++k) {
        vector.add(ran.nextFloat());
      }
      book_intro_array.add(vector);
    }

    List<InsertParam.Field> fields = new ArrayList<>();
    fields.add(new InsertParam.Field("book_id", book_id_array));
    fields.add(new InsertParam.Field("word_count", word_count_array));
    fields.add(new InsertParam.Field("book_intro", book_intro_array));

    InsertParam insertParam = InsertParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .withFields(fields)
        .build();
    milvusServiceClient.insert(insertParam);

    FlushParam flushParam = FlushParam.newBuilder()
        .withCollectionNames(Lists.newArrayList(COLLECTION_NAME))
        .build();
    milvusServiceClient.flush(flushParam);

    LoadCollectionParam loadCollectionParam = LoadCollectionParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();
    R<RpcStatus> loadStatus = milvusServiceClient.loadCollection(
        loadCollectionParam);
    System.out.println("load collection status: " + loadStatus);

    Integer SEARCH_K = 5;
    String SEARCH_PARAM = "{\"nprobe\":10, \"offset\":5}";

    List<String> search_output_fields = Arrays.asList("book_id");
    List<List<Float>> search_vectors = Arrays.asList(Arrays.asList(0.1f, 0.2f));

    SearchParam searchParam = SearchParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
        .withMetricType(MetricType.L2)
        .withOutFields(search_output_fields)
        .withTopK(SEARCH_K)
        .withVectors(search_vectors)
        .withVectorFieldName("book_intro")
        .withExpr("word_count <= 11000")
        .withParams(SEARCH_PARAM)
        .build();
    R<SearchResults> respSearch = milvusServiceClient.search(searchParam);

    SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(respSearch.getData().getResults());
    System.out.println(wrapperSearch.getIDScore(0));
    System.out.println(wrapperSearch.getFieldData("book_id", 0));

    List<String> query_output_fields = Arrays.asList("book_id", "word_count");
    QueryParam queryParam = QueryParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .withConsistencyLevel(ConsistencyLevelEnum.STRONG)
        .withExpr("book_id in [2,4,6,8]")
        .withOutFields(query_output_fields)
        .withOffset(0L)
        .withLimit(10L)
        .build();
    R<QueryResults> respQuery = milvusServiceClient.query(queryParam);

    QueryResultsWrapper wrapperQuery = new QueryResultsWrapper(respQuery.getData());
    System.out.println(wrapperQuery.getFieldWrapper("book_id").getFieldData());
    System.out.println(wrapperQuery.getFieldWrapper("word_count").getFieldData());

    milvusServiceClient.releaseCollection(
        ReleaseCollectionParam.newBuilder()
            .withCollectionName(COLLECTION_NAME)
            .build());
  }

  private static void createCollection() {
    FieldType fieldType1 = FieldType.newBuilder()
        .withName("book_id")
        .withDataType(DataType.Int64)
        .withPrimaryKey(true)
        .withAutoID(false)
        .build();
    FieldType fieldType2 = FieldType.newBuilder()
        .withName("word_count")
        .withDataType(DataType.Int64)
        .build();
    FieldType fieldType3 = FieldType.newBuilder()
        .withName("book_intro")
        .withDataType(DataType.FloatVector)
        .withDimension(2)
        .build();
    CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .withDescription("Test book search")
        .withShardsNum(2)
        .addFieldType(fieldType1)
        .addFieldType(fieldType2)
        .addFieldType(fieldType3)
//        .withEnableDynamicField(true)
        .build();

    milvusServiceClient.createCollection(createCollectionReq);

    milvusServiceClient.createIndex(
        CreateIndexParam.newBuilder()
            .withCollectionName(COLLECTION_NAME)
            .withFieldName("book_intro")
            .withIndexType(IndexType.IVF_FLAT)
            .withMetricType(MetricType.L2)
            .withExtraParam("{\"nlist\":1024}")
            .withSyncMode(Boolean.FALSE)
            .build()
    );
  }

  private static void dropCollection() {
    DropCollectionParam dropCollectionParam = DropCollectionParam.newBuilder()
        .withCollectionName(COLLECTION_NAME)
        .build();
    milvusServiceClient.dropCollection(dropCollectionParam);
  }

}