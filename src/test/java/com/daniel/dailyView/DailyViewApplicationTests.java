package com.daniel.dailyView;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"app.screener.alpha-futures.refresh-enabled=false",
		"app.screener.alpha-futures.refresh-run-on-startup=false"
})
class DailyViewApplicationTests {

	@Test
	void contextLoads() {
	}

}
