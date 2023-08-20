package com.apolloconfig.apollo.ai.qabot.api;

import com.apolloconfig.apollo.ai.qabot.markdown.MarkdownSearchResult;
import com.theokanning.openai.embedding.Embedding;
import java.util.List;

public interface VectorDBService {

  void persistChunkEmbeddings(String fileRoot, List<String> chunks,
      List<Embedding> embeddings);

  List<MarkdownSearchResult> search(List<List<Float>> searchVectors, int topK);

  String queryFileHashValue(String fileRoot);

  void persistFile(String fileRoot, String hashValue);
}
