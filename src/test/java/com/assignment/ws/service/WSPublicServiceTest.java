package com.assignment.ws.service;
import com.assignment.orderbook.OrderbookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.IOException;
import java.util.concurrent.*;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WSPublicServiceTest {

    @Mock
    private StandardWebSocketClient wsClient;

    @Mock
    private WSPublicHandler publicHandler;

    @Mock
    private OrderbookService orderbookService;

    @Mock
    private WebSocketSession webSocketSession;

    @InjectMocks
    private WSPublicService wsPublicService;

    @Test
    void testConnect() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());

        boolean result = wsPublicService.connect();

        assertTrue(result);
        verify(wsClient, times(1)).execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class));
    }

    @Test
    void testConnect_exception(){
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenThrow(RuntimeException.class);

        boolean result = wsPublicService.connect();

        assertFalse(result);
        verify(wsClient, times(1)).execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class));
    }

    @Test
    void testConnectAlreadyConnected() throws Exception {
        when(webSocketSession.isOpen()).thenReturn(true);
        ReflectionTestUtils.setField(wsPublicService, "currentSession", webSocketSession);
        boolean result = wsPublicService.connect();

        assertTrue(result);
        verify(wsClient, times(0)).execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class));
    }

    @Test
    void testConnected() {
        when(webSocketSession.isOpen()).thenReturn(true);
        ReflectionTestUtils.setField(wsPublicService, "currentSession", webSocketSession);
        boolean result = wsPublicService.connected();

        assertTrue(result);
    }

    @Test
    void testSubscribe() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        Map map = Mockito.mock(Map.class);
        when(publicHandler.getErrorMessages()).thenReturn(map);
        when(map.get(anyInt())).thenReturn(mockErrorMessages());

        wsPublicService.connect();
        wsPublicService.subscribe(List.of("symbol1", "symbol2"), "channel");

        verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSubscribe_invalidRequest1() throws Exception {
        assertThrows(RuntimeException.class, () -> wsPublicService.subscribe(null, "channel"));
    }

    @Test
    void testSubscribe_invalidRequest2() throws Exception {
        assertThrows(RuntimeException.class, () -> wsPublicService.subscribe(List.of("symbol1", "symbol2"), ""));
    }

    @Test
    void testSubscribe_exception_no_response() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        Map map = Mockito.mock(Map.class);
        when(publicHandler.getErrorMessages()).thenReturn(map);

        wsPublicService.connect();
        assertThrows(RuntimeException.class, () -> wsPublicService.subscribe(List.of("symbol1", "symbol2"), "channel"));

        verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSubscribe_exception_missing_response() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        Map map = Mockito.mock(Map.class);
        when(publicHandler.getErrorMessages()).thenReturn(map);

        Map<String, String> messages = mockErrorMessages();
        messages.remove("symbol1");
        when(map.get(anyInt())).thenReturn(messages);

        wsPublicService.connect();
        assertThrows(RuntimeException.class, () -> wsPublicService.subscribe(List.of("symbol1", "symbol2"), "channel"));

        verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSubscribe_exception_error_response() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        Map map = Mockito.mock(Map.class);
        when(publicHandler.getErrorMessages()).thenReturn(map);

        Map<String, String> messages = mockErrorMessages();
        messages.put("symbol1", "ERROR");
        when(map.get(anyInt())).thenReturn(messages);

        wsPublicService.connect();
        wsPublicService.subscribe(List.of("symbol1", "symbol2"), "channel");

        verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testSubscribe_exception_sendMessage() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        doThrow(IOException.class).when(webSocketSession).sendMessage(any());

        wsPublicService.connect();
        assertThrows(RuntimeException.class, () -> wsPublicService.subscribe(List.of("symbol1", "symbol2"), "channel"));
        verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testUnsubscribe() throws Exception {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        Map map = Mockito.mock(Map.class);
        when(publicHandler.getErrorMessages()).thenReturn(map);
        when(map.get(anyInt())).thenReturn(mockErrorMessages());

        wsPublicService.connect();
        wsPublicService.unsubscribe(List.of("symbol1", "symbol2"), "channel");

        verify(webSocketSession, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void testClose() throws IOException {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        wsPublicService.connect();
        wsPublicService.close();
        verify(webSocketSession, times(1)).close();
    }

    @Test
    void testClose_exception() throws IOException {
        when(wsClient.execute(any(TextWebSocketHandler.class), any(WebSocketHttpHeaders.class), any(URI.class)))
                .thenReturn(completableFutureWithSession());
        wsPublicService.connect();

        doThrow(IOException.class).when(webSocketSession).close();
        assertThrows(RuntimeException.class, () -> wsPublicService.close());
    }

    private CompletableFuture<WebSocketSession> completableFutureWithSession() {
        CompletableFuture<WebSocketSession> future = new CompletableFuture<>();
        future.complete(webSocketSession);
        return future;
    }

    private Map<String, String>  mockErrorMessages() {
        Map<String, String> messages = new HashMap<>();
        messages.put("symbol1", "Success");
        messages.put("symbol2", "Success");
        return messages;
    }
}
