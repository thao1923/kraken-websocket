package com.assignment.kafka.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;


@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaService {
    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;
    @Getter
    private final CountDownLatch latch = new CountDownLatch(3);

    public void publish(String topic, String value) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, value);
        future.whenComplete((result, e) -> {
            if (e == null) {
                log.info("Message sent success to kafka topic {} partition {} offset {}", result.getRecordMetadata().topic(), result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                log.error("Message sending failed with{}", e.getMessage());
            }
        });
    }


    @KafkaListener(topics = "orderbook-candle-data", groupId = "candle-consumer")
    public void consume(String message){
        log.info("Received message from Kafka: {}", message);
        latch.countDown();
    }

}
