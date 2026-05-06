package com.market_streaming;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class MarketDataStreamingApplicationTest {
    @Test
    void contextLoads() {
    }

    @Test
    void testMain() {
        MarketDataStreamingApplication.main(new String[] {});
    }
}