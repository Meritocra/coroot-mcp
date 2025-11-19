package com.meritocra.corootmcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Logs a warning when the application is running against a real Coroot backend
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
			logger.warn("OPENAI_API_KEY (spring.ai.openai.api-key) is not configured. "
					+ "Summarization tools depending on Spring AI will fail when called.");
		}
	}

}
