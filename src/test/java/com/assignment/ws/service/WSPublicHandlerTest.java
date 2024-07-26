package com.assignment.ws.service;

import com.assignment.orderbook.OrderbookService;
import com.assignment.util.JsonUtil;
import com.assignment.util.ReadFile;
import com.assignment.ws.dto.BookAckSubscribeDto;
import com.assignment.ws.dto.WSAckMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.web.socket.CloseStatus;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
class WSPublicHandlerTest {

    @Mock
    private OrderbookService orderbookService;

    @Mock
    private WebSocketSession webSocketSession;

    @InjectMocks
    private WSPublicHandler wsPublicHandler;

    private static final String SRC_TEST_RESOURCES_JSON_PATH = "src/test/resources/";

    @Test
    void testAfterConnectionEstablished() throws Exception {
        when(webSocketSession.getId()).thenReturn("12345");

        wsPublicHandler.afterConnectionEstablished(webSocketSession);

        // Add verification of the log message if necessary
        verify(webSocketSession, times(1)).getId();
    }

    @Test
    void testHandleTextMessage_SuccessSubscribe() {
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/subscribe-ack.json");
        TextMessage message = new TextMessage(data);

        wsPublicHandler.handleTextMessage(webSocketSession, message);
        Map<Integer, Map<String, String>> errorMessages = wsPublicHandler.getErrorMessages();
        assertTrue(errorMessages.containsKey(1));
        assertEquals("success", errorMessages.get(1).get("ALGO/USD"));
    }

    @Test
    void testHandleTextMessage_SuccessUnsubscribe() {
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/unsubscribe-ack.json");
        TextMessage message = new TextMessage(data);

        wsPublicHandler.handleTextMessage(webSocketSession, message);

        Map<Integer, Map<String, String>> errorMessages = wsPublicHandler.getErrorMessages();
        assertTrue(errorMessages.containsKey(1));
        assertEquals("success", errorMessages.get(1).get("ALGO/USD"));
    }

    @Test
    void testHandleTextMessage_Error() {
        String payload = "{\"req_id\":3,\"result\":{\"symbol\":\"symbol3\"},\"success\":false,\"error\":\"error\"}";
        TextMessage message = new TextMessage(payload);

        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/unsubscribe-ack.json");
        WSAckMessage<?> ack = JsonUtil.parseJson(data, WSAckMessage.class, BookAckSubscribeDto.class);
        ack.setSuccess(false);
        ack.setError("ERROR");

        try (MockedStatic<JsonUtil> jsonUtil = Mockito.mockStatic(JsonUtil.class)) {
            jsonUtil.when(() -> JsonUtil.parseJson(any(), any(), any())).thenReturn(ack);
            wsPublicHandler.handleTextMessage(webSocketSession, message);
            Map<Integer, Map<String, String>> errorMessages = wsPublicHandler.getErrorMessages();
            assertTrue(errorMessages.containsKey(1));
            assertEquals("ERROR", errorMessages.get(1).get("ALGO/USD"));
        }
    }

    @Test
    void testHandleTextMessage_null() {
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/subscribe-ack.json");
        TextMessage message = new TextMessage(data);

        try (MockedStatic<JsonUtil> jsonUtil = Mockito.mockStatic(JsonUtil.class)) {
            jsonUtil.when(() -> JsonUtil.parseJson(any(), any(), any())).thenReturn(null);
            assertThrows(RuntimeException.class, () -> wsPublicHandler.handleTextMessage(webSocketSession, message));
        }

    }

    @Test
    void testHandleTextMessage_BookResponse() {
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        TextMessage message = new TextMessage(data);
        doNothing().when(orderbookService).apply(any(), any());
        wsPublicHandler.handleTextMessage(webSocketSession, message);
        verify(orderbookService, times(1)).apply(any(), any());
    }

    @Test
    void testHandleTextMessage_BookResponse_null() {
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        TextMessage message = new TextMessage(data);

        try (MockedStatic<JsonUtil> jsonUtil = Mockito.mockStatic(JsonUtil.class)) {
            jsonUtil.when(() -> JsonUtil.parseJson(any(), any())).thenReturn(null);
            assertThrows(RuntimeException.class, () -> wsPublicHandler.handleTextMessage(webSocketSession, message));
        }
    }

    @Test
    void testHandleTransportError() {
        Throwable exception = new RuntimeException("Transport error");

        wsPublicHandler.handleTransportError(webSocketSession, exception);

        // Add verification of the log message if necessary
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        when(webSocketSession.getId()).thenReturn("12345");
        CloseStatus status = new CloseStatus(1000);

        wsPublicHandler.afterConnectionClosed(webSocketSession, status);

        // Add verification of the log message if necessary
        verify(webSocketSession, times(1)).getId();
    }
}
