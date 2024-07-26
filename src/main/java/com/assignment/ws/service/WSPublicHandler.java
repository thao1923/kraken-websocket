package com.assignment.ws.service;

import com.assignment.orderbook.OrderbookService;
import com.assignment.util.CommonUtil;
import com.assignment.util.JsonUtil;
import com.assignment.ws.dto.BookAckSubscribeDto;
import com.assignment.ws.dto.BookAckUnsubDto;
import com.assignment.ws.dto.WSBookResponse;
import com.assignment.ws.dto.WSAckMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@RequiredArgsConstructor
@Component
public class WSPublicHandler extends TextWebSocketHandler {
    private final Map<Integer, Map<String, String>> errorMessages = new HashMap<>(500);
    private final OrderbookService orderbookService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("Connected to WebSocket server. Session Id {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.debug("Received: {}", payload);

        if (payload.contains("success")){
            WSAckMessage<?> ackMessage;
            if (payload.contains("unsubscribe")){
                ackMessage = JsonUtil.parseJson(payload, WSAckMessage.class, BookAckUnsubDto.class);
            }else{
                ackMessage = JsonUtil.parseJson(payload, WSAckMessage.class, BookAckSubscribeDto.class);
            }
            if (ackMessage == null){
                throw new RuntimeException("cannot read received data");
            }

            String symbol = (String) CommonUtil.getFieldByFieldName(ackMessage.getResult(), "symbol");
            if (ackMessage.isSuccess()){
                errorMessages.computeIfAbsent(ackMessage.getReq_id(), k -> new HashMap<>()).put(symbol, "success");
            }else {
                errorMessages.computeIfAbsent(ackMessage.getReq_id(), k -> new HashMap<>()).put(symbol, ackMessage.getError());
            }
        } else if (payload.contains("book")) {
            WSBookResponse response = JsonUtil.parseJson(payload, WSBookResponse.class);
            if (response == null){
                throw new RuntimeException("cannot read received data");
            }
            orderbookService.apply(response.getData(), response.getType());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        System.out.println("Error: " + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Kraken ws: connection closed {}", session.getId());
    }






}
