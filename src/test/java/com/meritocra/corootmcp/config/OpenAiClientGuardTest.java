package com.meritocra.corootmcp.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OpenAiClientGuardTest {

	@Test
	void givenMissingApiKey_whenGuardRuns_thenThrowsIllegalState() {
		// given
		OpenAiProperties properties = new OpenAiProperties();
		OpenAiClientGuard guard = new OpenAiClientGuard(properties);

		// when / then
		assertThatThrownBy(guard::afterSingletonsInstantiated)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("OPENAI_API_KEY");
	}

	@Test
	void givenApiKeyConfigured_whenGuardRuns_thenDoesNotThrow() {
		// given
		OpenAiProperties properties = new OpenAiProperties();
		properties.setApiKey("test-key");

		OpenAiClientGuard guard = new OpenAiClientGuard(properties);

		// when / then
		assertThatCode(guard::afterSingletonsInstantiated)
				.doesNotThrowAnyException();
	}

}

