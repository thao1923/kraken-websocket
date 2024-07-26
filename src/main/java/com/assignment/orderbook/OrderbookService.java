package com.assignment.orderbook;

import com.assignment.dto.Candle;
import com.assignment.dto.Level;
import com.assignment.dto.Orderbook;
import com.assignment.kafka.service.KafkaService;
import com.assignment.constants.Constant;
import com.assignment.util.JsonUtil;
import com.assignment.ws.dto.BookData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderbookService {
    private final OrderbookHelperService orderbookHelperService;
    private final KafkaService kafkaService;
    protected final Map<String, Orderbook> mainOrderbook = new HashMap<>();
    protected final Map<String, Map<BigDecimal, Level>> priceLevels = new HashMap<>();
    private final Map<String, Candle> candleData = new HashMap<>();
    private final Map<String, Candle> prevCandleData = new HashMap<>();
    private LocalDateTime start;

    public void apply(List<BookData> receivedData, String type){

        if ("snapshot".equals(type)){
            processSnapshot(receivedData);
        }else if ("update".equals(type)){
            processDelta(receivedData);
        }
    }

    public void clearData(List<String> symbols){
        for (String symbol: symbols){
            mainOrderbook.remove(symbol);
            candleData.remove(symbol);
            prevCandleData.remove(symbol);
            priceLevels.remove(symbol);
        }
    }


    public void processSnapshot(List<BookData> receivedData){
        log.info("Snapshot received");
        LocalDateTime now = LocalDateTime.now();
        if (mainOrderbook.isEmpty()) start = now;

        for (BookData data: receivedData){
            if (CollectionUtils.isEmpty(data.getAsks()) || CollectionUtils.isEmpty(data.getBids())) throw new RuntimeException("There must be always one bid and ask present");
            if (data.getBids().get(0).compareTo(data.getAsks().get(0)) <= 0) {
                throw new RuntimeException("Highest bid must be smaller than lowest ask");
            }

            TreeSet<Level> asks = new TreeSet<>(data.getAsks());
            TreeSet<Level> bids = new TreeSet<>(data.getBids());

            mainOrderbook.computeIfAbsent(data.getSymbol(), k -> new Orderbook(new TreeSet<>(), new TreeSet<>())).getAsks().addAll(asks);
            mainOrderbook.computeIfAbsent(data.getSymbol(), k -> new Orderbook(new TreeSet<>(), new TreeSet<>())).getBids().addAll(bids);
            orderbookHelperService.removeExtraLevels(mainOrderbook.get(data.getSymbol()).getBids(), mainOrderbook.get(data.getSymbol()).getAsks(), priceLevels.get(data.getSymbol()), Constant.PRICE_DEPTH);

            data.getAsks().addAll(data.getBids());
            orderbookHelperService.buildPriceLevelsMap(data.getSymbol(), data.getAsks(), priceLevels);

            if (!orderbookHelperService.checkSum(mainOrderbook.get(data.getSymbol()).getAsks(), mainOrderbook.get(data.getSymbol()).getBids(), data.getChecksum())) {
                throw new RuntimeException(String.format("Checksum Failed at %s with states [asks: %s, bids: %s] and checksum %s", data.getTimestamp(), mainOrderbook.get(data.getSymbol()).getAsks(), mainOrderbook.get(data.getSymbol()).getBids(), data.getChecksum()));
            }

            candleData.putIfAbsent(data.getSymbol(), Candle.builder().timestamp(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).ticks(0L).build());
            orderbookHelperService.buildCandle(candleData.get(data.getSymbol()), start, now, mainOrderbook.get(data.getSymbol()).getBids().first().getPrice(), mainOrderbook.get(data.getSymbol()).getAsks().last().getPrice());
        }

        if (start != null && now.isAfter(start.plusMinutes(1L))){
            sendToKafkaAndResetCandle(now);
        }

        log.info("Orderbook after processing snapshot {}", mainOrderbook);
    }

    public void processDelta(List<BookData> receivedData) {
        log.info("Delta message received");
        LocalDateTime now = LocalDateTime.now();
        for (BookData data : receivedData) {
            orderbookHelperService.processLevelData(data.getAsks(), priceLevels.getOrDefault(data.getSymbol(), new HashMap<>()), mainOrderbook.get(data.getSymbol()).getAsks());
            orderbookHelperService.processLevelData(data.getBids(), priceLevels.getOrDefault(data.getSymbol(), new HashMap<>()), mainOrderbook.get(data.getSymbol()).getBids());

            orderbookHelperService.removeExtraLevels(mainOrderbook.get(data.getSymbol()).getBids(), mainOrderbook.get(data.getSymbol()).getAsks(), priceLevels.get(data.getSymbol()), Constant.PRICE_DEPTH);

            if (mainOrderbook.get(data.getSymbol()).getBids().first().compareTo(mainOrderbook.get(data.getSymbol()).getAsks().last()) <= 0) {
                throw new RuntimeException("Highest bid must be smaller than lowest ask");
            }

            if (!orderbookHelperService.checkSum(mainOrderbook.get(data.getSymbol()).getAsks(), mainOrderbook.get(data.getSymbol()).getBids(), data.getChecksum())) {
                throw new RuntimeException(String.format("Checksum Failed at %s with states [asks: %s, bids: %s] with checksum %s", now, mainOrderbook.get(data.getSymbol()).getAsks(), mainOrderbook.get(data.getSymbol()).getBids(), data.getChecksum()));
            }

            orderbookHelperService.buildCandle(candleData.get(data.getSymbol()), start, now, mainOrderbook.get(data.getSymbol()).getBids().first().getPrice(), mainOrderbook.get(data.getSymbol()).getAsks().last().getPrice());
        }
        if (start != null && now.isAfter(start.plusMinutes(1L))){
            sendToKafkaAndResetCandle(now);
        }
        log.info("Orderbook after processing delta {}", mainOrderbook);
    }

    public Candle getCandle(String symbol){
        if(!orderbookHelperService.isInvalidCandle(candleData.get(symbol))){
            return candleData.get(symbol);
        }else{
            return prevCandleData.get(symbol);
        }
    }

    private void sendToKafkaAndResetCandle(LocalDateTime now){
        start = now;
        for (String symbol: candleData.keySet()) {
            kafkaService.publish(Constant.KAFKA_TOPIC, JsonUtil.toJson(candleData.get(symbol)));
            prevCandleData.put(symbol, candleData.get(symbol));
            candleData.put(symbol, Candle.builder().timestamp(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).open(prevCandleData.get(symbol).getClose()).ticks(0L).build());
        }
    }
}
