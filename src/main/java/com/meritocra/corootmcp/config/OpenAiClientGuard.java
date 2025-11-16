package com.meritocra.corootmcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails fast when the application is running against a real Coroot backend
 * but the OpenAI API key is not configured.
 *
 * <p>
 * This guard is disabled for the {@code stub-coroot} profile so local demos can
 * start without external credentials. Summarization tools will still fail at
 * call time if no key is provided.
 * </p>
 */
@Component
@Profile("!stub-coroot")
public class OpenAiClientGuard implements SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(OpenAiClientGuard.class);

	private final OpenAiProperties properties;

	public OpenAiClientGuard(OpenAiProperties properties) {
		this.properties = properties;
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (!StringUtils.hasText(properties.getApiKey())) {
			logger.error("OPENAI_API_KEY (spring.ai.openai.api-key) must be configured to use summarization tools.");
			throw new IllegalStateException(
					"OPENAI_API_KEY (spring.ai.openai.api-key) must be configured to use summarization tools.");
		}
	}

}

