package com.meritocra.corootmcp.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

class CorootClientGuardTest {

	@Test
	void givenMissingConfig_whenGuardRuns_thenThrowsIllegalState() {
		// given
		CorootProperties properties = new CorootProperties();
		CorootClientGuard guard = new CorootClientGuard(properties);

		// when / then
		assertThatThrownBy(guard::afterSingletonsInstantiated)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("COROOT_API_URL");
	}

	@Test
	void givenApiUrlAndKeyConfigured_whenGuardRuns_thenDoesNotThrow() {
		// given
		CorootProperties properties = new CorootProperties();
		properties.setApiUrl(URI.create("https://coroot.example.com"));
		properties.setApiKey("test-key");

		CorootClientGuard guard = new CorootClientGuard(properties);

		// when / then
		assertThatCode(guard::afterSingletonsInstantiated)
				.doesNotThrowAnyException();
	}

}

