package com.apolloconfig.apollo.ai.qabot.api;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.Embedding;
import java.util.List;

public interface AiService {

  String getCompletion(String prompt);

  String getCompletionFromMessages(List<ChatMessage> messages);

  List<Embedding> getEmbeddings(List<String> chunks);

}
