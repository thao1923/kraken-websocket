package com.assignment.orderbook;

import com.assignment.dto.Candle;
import com.assignment.dto.Level;
import com.assignment.util.JsonUtil;
import com.assignment.util.ReadFile;
import com.assignment.ws.dto.WSBookResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderbookHelperServiceTest {

    @InjectMocks
    OrderbookHelperService orderbookHelperService;

    private static final String SRC_TEST_RESOURCES_JSON_PATH = "src/test/resources/";

    @Test
    void testGenerateStrForCheckSum(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot-checksum.json");
        WSBookResponse snapshot = JsonUtil.parseJson(data, WSBookResponse.class);

        TreeSet<Level> asks = new TreeSet<>(snapshot.getData().get(0).getAsks());
        TreeSet<Level> bids = new TreeSet<>(snapshot.getData().get(0).getBids());

        assertTrue(orderbookHelperService.checkSum(asks, bids, snapshot.getData().get(0).getChecksum()));
    }

    @Test
    void testGenerateStrForCheckSum_false(){
        String data = ReadFile.readFile(SRC_TEST_RESOURCES_JSON_PATH + "mock-data/snapshot-checksum.json");
        WSBookResponse snapshot = JsonUtil.parseJson(data, WSBookResponse.class);

        TreeSet<Level> asks = new TreeSet<>(snapshot.getData().get(0).getAsks());
        TreeSet<Level> bids = new TreeSet<>(snapshot.getData().get(0).getBids());
        bids.remove(bids.last());

        assertFalse(orderbookHelperService.checkSum(asks, bids, snapshot.getData().get(0).getChecksum()));
    }

    @Test
    void testIsInvalidCandle(){
        assertTrue(orderbookHelperService.isInvalidCandle(new Candle()));

        Candle candle = Candle.builder().low(BigDecimal.ONE).open(BigDecimal.ONE).close(BigDecimal.ONE).high(BigDecimal.ONE).ticks(2L).timestamp(123L).build();
        assertFalse(orderbookHelperService.isInvalidCandle(candle));
    }

    @Test
    void testBuildPriceLevelsMap(){
        Map<String, Map<BigDecimal, Level>> priceLevels = new HashMap<>();
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();

        List<Level> priceLevelList = List.of(level1, level2);
        orderbookHelperService.buildPriceLevelsMap("TEST",priceLevelList, priceLevels);
        assertEquals(2, priceLevels.get("TEST").size());
    }

    @Test
    void testRemoveLevel(){
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();

        TreeSet<Level> set = new TreeSet<>(List.of(level1, level2));
        Map<BigDecimal, Level> map = new HashMap<>();
        map.put(level1.getPrice(), level1);
        map.put(level2.getPrice(), level2);

        orderbookHelperService.removeLevel(new Level(BigDecimal.valueOf(0.5678), BigDecimal.TEN), set, map);
        assertEquals(1, set.size());
        assertEquals(1, map.size());
    }

    @Test
    void testRemoveLevel_null(){
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();

        TreeSet<Level> set = new TreeSet<>(List.of(level1, level2));
        Map<BigDecimal, Level> map = new HashMap<>();
        map.put(level1.getPrice(), level1);
        map.put(level2.getPrice(), level2);

        orderbookHelperService.removeLevel(null, set, map);
        assertEquals(2, set.size());
        assertEquals(2, map.size());
    }

    @Test
    void insertLevel(){
        TreeSet<Level> set = new TreeSet<>();
        Map<BigDecimal, Level> map = new HashMap<>();
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        orderbookHelperService.insertLevel(null, level1, set, map);
        assertEquals(1, set.size());
        assertEquals(1, map.size());
    }

    @Test
    void testRemoveExtraLevel(){
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();
        Level level3 = Level.builder().price(BigDecimal.valueOf(0.7687)).qty(BigDecimal.ONE).build();

        TreeSet<Level> bids = new TreeSet<>(List.of(level1, level2, level3));
        Map<BigDecimal, Level> map = new HashMap<>();
        map.put(level1.getPrice(), level1);
        map.put(level2.getPrice(), level2);
        map.put(level3.getPrice(), level3);

        Level level4 = Level.builder().price(BigDecimal.valueOf(0.7787)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level5 = Level.builder().price(BigDecimal.valueOf(0.7789)).qty(BigDecimal.ZERO).build();
        Level level6 = Level.builder().price(BigDecimal.valueOf(0.7790)).qty(BigDecimal.ONE).build();
        TreeSet<Level> asks = new TreeSet<>(List.of(level4, level5, level6));
        map.put(level4.getPrice(), level4);
        map.put(level5.getPrice(), level5);
        map.put(level6.getPrice(), level6);

        orderbookHelperService.removeExtraLevels(bids, asks, map, 2);
        assertEquals(2, bids.size());
        assertEquals(2, asks.size());
        assertEquals(4, map.size());
    }

    @Test
    void testBuildCandle_open(){
        LocalDateTime now = LocalDateTime.now();
        Candle candle = Candle.builder().timestamp(now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).ticks(0L).build();
        orderbookHelperService.buildCandle(candle, now, now, BigDecimal.valueOf(0.5666), BigDecimal.valueOf(0.5668));
        assertEquals(candle.getOpen(), BigDecimal.valueOf(0.5667));
    }

    @Test
    void testBuildCandle_close1(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(1L);
        Candle candle = Candle.builder().timestamp(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).ticks(0L).build();
        orderbookHelperService.buildCandle(candle, start, now, BigDecimal.valueOf(0.5666), BigDecimal.valueOf(0.5668));
        assertEquals(candle.getClose(), BigDecimal.valueOf(0.5667));
    }

    @Test
    void testBuildCandle_close2(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(1L).minusSeconds(20L);
        Candle candle = Candle.builder().timestamp(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).ticks(0L).build();
        orderbookHelperService.buildCandle(candle, start, now, BigDecimal.valueOf(0.5666), BigDecimal.valueOf(0.5668));
        assertEquals(candle.getClose(), BigDecimal.valueOf(0.5667));
    }

    @Test
    void testBuildCandle_nullHighAndLow(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusSeconds(20L);
        Candle candle = Candle.builder().timestamp(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).ticks(0L).build();
        orderbookHelperService.buildCandle(candle, start, now, BigDecimal.valueOf(0.5666), BigDecimal.valueOf(0.5668));
        assertEquals(candle.getHigh(), BigDecimal.valueOf(0.5667));
    }

    @Test
    void testBuildCandle(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusSeconds(20L);
        Candle candle = Candle.builder().timestamp(start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).ticks(1L).high(BigDecimal.ONE).low(BigDecimal.valueOf(0.6868)).build();
        orderbookHelperService.buildCandle(candle, start, now, BigDecimal.valueOf(0.5666), BigDecimal.valueOf(0.5668));
        assertEquals(candle.getLow(), BigDecimal.valueOf(0.5667));
    }

    @Test
    void testProcessLevelData(){
        Level level1 = Level.builder().price(BigDecimal.valueOf(0.5678)).qty(BigDecimal.valueOf(5000.45)).build();
        Level level2 = Level.builder().price(BigDecimal.valueOf(0.5677)).qty(BigDecimal.ZERO).build();

        TreeSet<Level> set = new TreeSet<>();
        Map<BigDecimal, Level> map = new HashMap<>();
        map.put(level1.getPrice(), level1);
        map.put(level2.getPrice(), level2);

        orderbookHelperService.processLevelData(List.of(level1, level2), map, set);
        assertEquals(1, set.size());
    }

}