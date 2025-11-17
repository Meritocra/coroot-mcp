package com.meritocra.corootmcp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fails fast when the application is running against a real Coroot backend
 * but required Coroot configuration is missing.
 *
 * <p>
 * This guard is disabled for the {@code stub-coroot} profile so local demos can
 * start without external Coroot credentials.
 * </p>
 */
@Component
@Profile("!stub-coroot")
public class CorootClientGuard implements SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(CorootClientGuard.class);

	private final CorootProperties properties;

	public CorootClientGuard(CorootProperties properties) {
		this.properties = properties;
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (properties.getApiUrl() == null) {
			logger.error("COROOT_API_URL (coroot.api-url) must be configured to talk to Coroot.");
			throw new IllegalStateException("COROOT_API_URL (coroot.api-url) must be configured to talk to Coroot.");
		}
		if (!StringUtils.hasText(properties.getApiKey())) {
			logger.error("COROOT_API_KEY (coroot.api-key) must be configured to call the Coroot HTTP API.");
			throw new IllegalStateException("COROOT_API_KEY (coroot.api-key) must be configured to call the Coroot HTTP API.");
		}
	}

}

