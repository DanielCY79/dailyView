package com.daniel.dailyView;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.flyway.enabled=false",
		"app.screener.alpha-futures.refresh-enabled=false",
		"app.screener.alpha-futures.refresh-run-on-startup=false"
})
class DailyViewApplicationTests {

	@Test
	void contextLoads() {
	}

}
