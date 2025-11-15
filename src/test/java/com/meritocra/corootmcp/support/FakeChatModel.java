package com.meritocra.corootmcp.support;

import java.util.List;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

public class FakeChatModel implements ChatModel {

	@Override
	public ChatResponse call(Prompt prompt) {
		Generation generation = new Generation(new AssistantMessage("FAKE_SUMMARY"));
		return new ChatResponse(List.of(generation));
	}
}
