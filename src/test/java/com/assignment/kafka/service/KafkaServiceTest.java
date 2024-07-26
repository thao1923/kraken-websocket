package com.assignment.kafka.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
public class KafkaServiceTest {

    @InjectMocks
    private KafkaService kafkaService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;


    @Test
    public void testPublish() {
        String topic = "test-topic";
        String value = "test-message";

        SendResult<String, String> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(sendResult);

        when(kafkaTemplate.send(topic, value)).thenReturn(future);

        kafkaService.publish(topic, value);

        verify(kafkaTemplate, times(1)).send(topic, value);
    }

    @Test
    public void testPublish_failed() {
        String topic = "test-topic";
        String value = "test-message";
        Exception exception = new RuntimeException("Test exception");

        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(exception);

        when(kafkaTemplate.send(topic, value)).thenReturn(future);

        kafkaService.publish(topic, value);

        verify(kafkaTemplate, times(1)).send(topic, value);
    }

    @Test
    public void testConsume() throws InterruptedException {
        String message = "test-message";

        kafkaService.consume(message);

        assertEquals(2, kafkaService.getLatch().getCount(), "Latch count should be decremented by 1");
    }
}
