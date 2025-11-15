package com.meritocra.corootmcp.support;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestChatConfig {

	@Bean
	ChatModel chatModel() {
		return new FakeChatModel();
	}

}
