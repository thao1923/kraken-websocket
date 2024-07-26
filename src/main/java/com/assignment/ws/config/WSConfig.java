package com.assignment.ws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class WSConfig {

    @Bean
    StandardWebSocketClient wsClient(){
        return new StandardWebSocketClient();
    }
}
