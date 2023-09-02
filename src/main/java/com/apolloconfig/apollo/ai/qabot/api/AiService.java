package com.apolloconfig.apollo.ai.qabot.api;

import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.Embedding;
import io.reactivex.Flowable;
import java.util.List;

public interface AiService {

  Flowable<ChatCompletionChunk> getCompletion(String prompt);

  Flowable<ChatCompletionChunk> getCompletionFromMessages(List<ChatMessage> messages);

  List<Embedding> getEmbeddings(List<String> chunks);

}
