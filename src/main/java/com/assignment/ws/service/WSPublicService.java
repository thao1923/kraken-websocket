package com.assignment.ws.service;

import com.assignment.orderbook.OrderbookService;
import com.assignment.ws.dto.BookRequestParams;
import com.assignment.ws.dto.WSRequestMessage;
import com.assignment.util.JsonUtil;
import com.assignment.constants.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class WSPublicService {
    private final StandardWebSocketClient wsClient;
    private final WSPublicHandler publicHandler;
    private WebSocketSession currentSession;
    private final OrderbookService orderbookService;

    public boolean connect(){
        try{
            if (!connected()) {
                this.currentSession = wsClient.execute(publicHandler, new WebSocketHttpHeaders(), URI.create(Constant.PUBLIC_URL)).get();
            }
            return true;
        }catch (Exception e){
            log.error("Unable to connect to WebSocket API");
        }
        return false;
    }

    public boolean connected () {
        return this.currentSession != null && this.currentSession.isOpen();
    }

    private void sendAndAck(String msg, int reqId, int cntSymbol){
        int i = 1;
        Map<String, String> errorMsg = null;
        try{
            this.currentSession.sendMessage(new TextMessage(msg));
            do {
                errorMsg = publicHandler.getErrorMessages().get(reqId);
                Thread.sleep(i * 1000L);
            }
            while (i++ < 4 && (errorMsg == null || errorMsg.size() < cntSymbol));
        }catch (IOException | InterruptedException e){
            throw new RuntimeException(e.getMessage());
        }

        if (errorMsg == null || errorMsg.size() < cntSymbol) {
            throw new RuntimeException(String.format("Timeout while waiting for reqId=%s response", reqId));
        } else {
            for (String symbol: errorMsg.keySet()){
                if (!"success".equals(errorMsg.get(symbol))){
                    log.error("Error when acknowledge {}", errorMsg.get(symbol));
                }
            }
        }
    }

    private void validateRequest(List<String> symbol, String channel){
        if (CollectionUtils.isEmpty(symbol) || !StringUtils.hasText(channel)){
            throw new RuntimeException("Invalid input");
        }
    }


    public void subscribe(List<String> symbol, String channel){
        validateRequest(symbol, channel);
        int reqId = Math.abs(new Random().nextInt());

        BookRequestParams params = new BookRequestParams();
        params.setChannel(channel);
        params.setSymbol(symbol);

        WSRequestMessage<BookRequestParams> sendMsg = new WSRequestMessage<>();
        sendMsg.setParams(params);
        sendMsg.setMethod("subscribe");
        sendMsg.setReq_id(reqId);

        sendAndAck(JsonUtil.toJson(sendMsg), reqId, symbol.size());
    }

    public void unsubscribe(List<String> symbol, String channel){
        validateRequest(symbol, channel);
        int reqId = Math.abs(new Random().nextInt());

        BookRequestParams params = new BookRequestParams();
        params.setChannel(channel);
        params.setSymbol(symbol);

        WSRequestMessage<BookRequestParams> sendMsg = new WSRequestMessage<>();
        sendMsg.setParams(params);
        sendMsg.setMethod("unsubscribe");
        sendMsg.setReq_id(reqId);

        sendAndAck(JsonUtil.toJson(sendMsg), reqId, symbol.size());
        orderbookService.clearData(symbol);
    }

    public void close () {
        try{
            this.currentSession.close();
        }catch (IOException e){
            throw new RuntimeException(e.getMessage());
        }

    }





}
