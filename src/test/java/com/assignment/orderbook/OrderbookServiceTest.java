package com.assignment.orderbook;

import com.assignment.dto.Candle;
import com.assignment.dto.Level;
import com.assignment.dto.Orderbook;
import com.assignment.kafka.service.KafkaService;
import com.assignment.util.JsonUtil;
import com.assignment.util.ReadFile;
import com.assignment.ws.dto.WSBookResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class OrderbookServiceTest {
    @Mock
    OrderbookHelperService orderbookHelperService;

    @Mock
    KafkaService kafkaService;

    @Mock
    Map<String, Candle> candleData;

    @Mock
    Map<String, Candle> prevCandleData;

    @InjectMocks
    OrderbookService orderbookService;

    private static final String SRC_TEST_RESOURCES_JSON_PATH = "src/test/resources/";

    @Test
    void testApplySnapshot(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);

        Mockito.doCallRealMethod().when(orderbookHelperService).buildPriceLevelsMap(any(), any(), any());
        Mockito.doNothing().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);

        orderbookService.apply(res.getData(), "snapshot");
        assertEquals(BigDecimal.valueOf(0.5677), orderbookService.mainOrderbook.get("MATIC/USD").getAsks().first().getPrice());
        assertEquals(BigDecimal.valueOf(0.5657), orderbookService.mainOrderbook.get("MATIC/USD").getBids().last().getPrice());
    }

    @Test
    void testApplyUpdate(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);

        Mockito.doCallRealMethod().when(orderbookHelperService).buildPriceLevelsMap(any(), any(), any());
        Mockito.doNothing().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);

        orderbookService.apply(res.getData(), "snapshot");

        data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/update.json");
        WSBookResponse update = JsonUtil.parseJson(data, WSBookResponse.class);

        Mockito.doCallRealMethod().when(orderbookHelperService).processLevelData(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeLevel(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).insertLevel(any(), any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeExtraLevels(any(), any(), any(), anyInt());

        Mockito.doNothing().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);

        orderbookService.apply(update.getData(), "update");
        assertEquals(BigDecimal.valueOf(1098.3947558), orderbookService.mainOrderbook.get("MATIC/USD").getBids().last().getQty());

    }


    @Test
    void testProcessSnapshot(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);

        Mockito.doCallRealMethod().when(orderbookHelperService).buildPriceLevelsMap(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);

        orderbookService.processSnapshot(res.getData());
        Map<String, Candle> cdata = (Map<String, Candle>) ReflectionTestUtils.getField(orderbookService, "candleData");
        assertEquals(BigDecimal.valueOf(0.5677), orderbookService.mainOrderbook.get("MATIC/USD").getAsks().first().getPrice());
        assertEquals(BigDecimal.valueOf(0.5657), orderbookService.mainOrderbook.get("MATIC/USD").getBids().last().getPrice());
        assertEquals(BigDecimal.valueOf(0.5667), cdata.get("MATIC/USD").getOpen());
    }

    @Test
    void testProcessSnapshot2(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);

        orderbookService.mainOrderbook.put("TEST", new Orderbook());
        Mockito.doCallRealMethod().when(orderbookHelperService).buildPriceLevelsMap(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(orderbookService, "start", LocalDateTime.now().minusMinutes(1L).minusSeconds(20L));

        orderbookService.processSnapshot(res.getData());
        assertEquals(BigDecimal.valueOf(0.5677), orderbookService.mainOrderbook.get("MATIC/USD").getAsks().first().getPrice());
        assertEquals(BigDecimal.valueOf(0.5657), orderbookService.mainOrderbook.get("MATIC/USD").getBids().last().getPrice());
    }

    @Test
    void testProcessSnapshot_checkSumFailed(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);

        Mockito.doCallRealMethod().when(orderbookHelperService).buildPriceLevelsMap(any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> orderbookService.processSnapshot(res.getData()));
    }

    @Test
    void testProcessSnapshot_noBids_provided(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);
        res.getData().get(0).setBids(new ArrayList<>());

        assertThrows(RuntimeException.class, () -> orderbookService.processSnapshot(res.getData()));
    }

    @Test
    void testProcessSnapshot_noAsks_provided(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);
        res.getData().get(0).setAsks(new ArrayList<>());

        assertThrows(RuntimeException.class, () -> orderbookService.processSnapshot(res.getData()));
    }

    @Test
    void testProcessSnapshot_lowestAskLteHighestBids(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);
        res.getData().get(0).setAsks(List.of(Level.builder().price(BigDecimal.ONE).qty(BigDecimal.ONE).build()));
        res.getData().get(0).setBids(List.of(Level.builder().price(BigDecimal.TEN).qty(BigDecimal.ONE).build()));
        assertThrows(RuntimeException.class, () -> orderbookService.processSnapshot(res.getData()));
    }

    @Test
    void testProcessDelta_bids(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse snapshot = JsonUtil.parseJson(data, WSBookResponse.class);

        TreeSet<Level> asks = new TreeSet<>(snapshot.getData().get(0).getAsks());
        TreeSet<Level> bids = new TreeSet<>(snapshot.getData().get(0).getBids());

        orderbookService.mainOrderbook.put(snapshot.getData().get(0).getSymbol(), Orderbook.builder().asks(asks).bids(bids).build());
        orderbookService.priceLevels.put(snapshot.getData().get(0).getSymbol(), new HashMap<>());
        for (Level ask: asks){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(ask.getPrice(), ask);
        }

        for (Level bid: bids){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(bid.getPrice(), bid);
        }

        data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/update.json");
        WSBookResponse update = JsonUtil.parseJson(data, WSBookResponse.class);

        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5667)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5666)).qty(BigDecimal.ZERO).build();
        update.getData().get(0).getBids().addAll(List.of(level1, level2));

        Mockito.doCallRealMethod().when(orderbookHelperService).processLevelData(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).insertLevel(any(), any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeLevel(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeExtraLevels(any(), any(), any(), anyInt());

        Mockito.doNothing().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(orderbookService, "start", LocalDateTime.now().minusMinutes(1L));
        ReflectionTestUtils.setField(orderbookService, "candleData", candleData);
        Mockito.when(candleData.keySet()).thenReturn(Set.of("MATIC/USD"));
        Mockito.when(candleData.get(anyString())).thenReturn(new Candle());

        orderbookService.processDelta(update.getData());
        assertEquals(BigDecimal.valueOf(1098.3947558), orderbookService.mainOrderbook.get("MATIC/USD").getBids().last().getQty());
        assertEquals(BigDecimal.valueOf(5000.45), orderbookService.mainOrderbook.get("MATIC/USD").getBids().first().getQty());

    }

    @Test
    void testProcessDelta_asks(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse snapshot = JsonUtil.parseJson(data, WSBookResponse.class);

        TreeSet<Level> asks = new TreeSet<>(snapshot.getData().get(0).getAsks());
        TreeSet<Level> bids = new TreeSet<>(snapshot.getData().get(0).getBids());

        orderbookService.mainOrderbook.put(snapshot.getData().get(0).getSymbol(), Orderbook.builder().asks(asks).bids(bids).build());
        orderbookService.priceLevels.put(snapshot.getData().get(0).getSymbol(), new HashMap<>());
        for (Level ask: asks){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(ask.getPrice(), ask);
        }

        for (Level bid: bids){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(bid.getPrice(), bid);
        }

        data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/update.json");
        WSBookResponse update = JsonUtil.parseJson(data, WSBookResponse.class);
        update.getData().get(0).setBids(new ArrayList<>());
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();
        update.getData().get(0).setAsks(List.of(level1, level2));
        Mockito.doCallRealMethod().when(orderbookHelperService).processLevelData(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).insertLevel(any(), any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeLevel(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeExtraLevels(any(), any(), any(), anyInt());

        Mockito.doNothing().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);
        ReflectionTestUtils.setField(orderbookService, "start", LocalDateTime.now().minusSeconds(20L));

        orderbookService.processDelta(update.getData());
        assertEquals(BigDecimal.valueOf(5000.45), orderbookService.mainOrderbook.get("MATIC/USD").getAsks().first().getQty());
        assertEquals(10, orderbookService.mainOrderbook.get("MATIC/USD").getAsks().size());
    }

    @Test
    void testProcessDelta_checkSumFailed(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse snapshot = JsonUtil.parseJson(data, WSBookResponse.class);

        TreeSet<Level> asks = new TreeSet<>(snapshot.getData().get(0).getAsks());
        TreeSet<Level> bids = new TreeSet<>(snapshot.getData().get(0).getBids());

        orderbookService.mainOrderbook.put(snapshot.getData().get(0).getSymbol(), Orderbook.builder().asks(asks).bids(bids).build());
        orderbookService.priceLevels.put(snapshot.getData().get(0).getSymbol(), new HashMap<>());
        for (Level ask: asks){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(ask.getPrice(), ask);
        }
        for (Level bid: bids){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(bid.getPrice(), bid);
        }
        data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/update.json");
        WSBookResponse update = JsonUtil.parseJson(data, WSBookResponse.class);
        update.getData().get(0).setBids(new ArrayList<>());
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();
        update.getData().get(0).setAsks(List.of(level1, level2));

        Mockito.doCallRealMethod().when(orderbookHelperService).processLevelData(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).insertLevel(any(), any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeLevel(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeExtraLevels(any(), any(), any(), anyInt());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> orderbookService.processDelta(update.getData()));
    }

    @Test
    void testProcessDelta_highestBidsException(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse snapshot = JsonUtil.parseJson(data, WSBookResponse.class);

        TreeSet<Level> asks = new TreeSet<>(snapshot.getData().get(0).getAsks());
        TreeSet<Level> bids = new TreeSet<>(snapshot.getData().get(0).getBids());

        orderbookService.mainOrderbook.put(snapshot.getData().get(0).getSymbol(), Orderbook.builder().asks(asks).bids(bids).build());
        orderbookService.priceLevels.put(snapshot.getData().get(0).getSymbol(), new HashMap<>());
        for (Level ask: asks){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(ask.getPrice(), ask);
        }

        for (Level bid: bids){
            orderbookService.priceLevels.get(snapshot.getData().get(0).getSymbol()).put(bid.getPrice(), bid);
        }

        data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/update.json");
        WSBookResponse update = JsonUtil.parseJson(data, WSBookResponse.class);

        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5668)).qty(BigDecimal.valueOf(5000.45)).build();
        update.getData().get(0).getBids().add(level1);

        Mockito.doCallRealMethod().when(orderbookHelperService).processLevelData(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).insertLevel(any(), any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeLevel(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).removeExtraLevels(any(), any(), any(), anyInt());

        assertThrows(RuntimeException.class, () -> orderbookService.processDelta(update.getData()));
    }

    @Test
    void testGetCandle() {
        Mockito.when(orderbookHelperService.isInvalidCandle(any())).thenReturn(false);
        ReflectionTestUtils.setField(orderbookService, "candleData", candleData);
        Mockito.when(candleData.get(anyString())).thenReturn(new Candle());
        assertNotNull(orderbookService.getCandle("TEST"));
    }

    @Test
    void testGetCandle_prev() {
        Mockito.when(orderbookHelperService.isInvalidCandle(any())).thenReturn(true);
        ReflectionTestUtils.setField(orderbookService, "candleData", candleData);
        ReflectionTestUtils.setField(orderbookService, "prevCandleData", prevCandleData);
        Mockito.when(candleData.get(anyString())).thenReturn(new Candle());
        Mockito.when(prevCandleData.get(anyString())).thenReturn(new Candle());
        assertNotNull(orderbookService.getCandle("TEST"));
    }

    @Test
    void testClearData(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot.json");
        WSBookResponse res = JsonUtil.parseJson(data, WSBookResponse.class);

        Mockito.doCallRealMethod().when(orderbookHelperService).buildPriceLevelsMap(any(), any(), any());
        Mockito.doCallRealMethod().when(orderbookHelperService).buildCandle(any(), any(), any(), any(), any());
        Mockito.when(orderbookHelperService.checkSum(any(), any(), any())).thenReturn(true);

        orderbookService.processSnapshot(res.getData());
        orderbookService.clearData(List.of("MATIC/USD"));
        assertTrue(orderbookService.mainOrderbook.isEmpty());
        assertTrue(orderbookService.priceLevels.isEmpty());
    }




}