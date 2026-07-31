package com.smartcampus;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文加载测试：依赖完整运行环境（PostgreSQL/Redis 等），CI 单测阶段跳过。
 * 其余约 52 个纯 Mockito 单测不依赖上下文，CI 正常运行。
 * 本地如需验证上下文，可执行：mvn -Dtest=SmartCampusBackendApplicationTests test
 */
@Disabled("需完整运行环境（DB/Redis），CI 单测阶段跳过")
@SpringBootTest
class SmartCampusBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
