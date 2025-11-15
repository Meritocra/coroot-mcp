package com.meritocra.corootmcp.coroot;

import com.meritocra.corootmcp.config.CorootProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CorootClientConfiguration {

	@Bean
	@Profile("stub-coroot")
	CorootClient stubCorootClient() {
		return new StubCorootClient();
	}

	@Bean
	@Profile("!stub-coroot")
	CorootClient httpCorootClient(CorootProperties properties) {
		return new HttpCorootClient(properties);
	}
}

