package com.meritocra.corootmcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.meritocra.corootmcp.support.TestChatConfig;

@SpringBootTest
@Import(TestChatConfig.class)
class CorootMcpApplicationTests {

	@Test
	void contextLoads() {
	}

}
