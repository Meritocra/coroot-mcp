package com.meritocra.corootmcp.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class OpenAiClientGuardTest {

	@Test
	void givenMissingApiKey_whenGuardRuns_thenDoesNotThrow() {
		// given
		OpenAiProperties properties = new OpenAiProperties();
		OpenAiClientGuard guard = new OpenAiClientGuard(properties);

		// when / then
		assertThatCode(guard::afterSingletonsInstantiated)
				.doesNotThrowAnyException();
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
