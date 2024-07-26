package com.assignment.orderbook;

import com.assignment.constants.Constant;
import com.assignment.dto.Candle;
import com.assignment.dto.Level;
import com.assignment.util.CommonUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

@Service
public class OrderbookHelperService {
    private String generateStrForCheckSum(BigDecimal num){
        String ret = String.valueOf(num);
        ret = ret.replace(".", "").replaceFirst("^0+(?!$)", "");
        return ret;
    }

    public boolean checkSum(TreeSet<Level> asks, TreeSet<Level> bids, Long checksum){
        StringBuilder askStr = new StringBuilder();
        for (Level ask: asks.descendingSet()){
            askStr.append(generateStrForCheckSum(ask.getPrice()));
            askStr.append(generateStrForCheckSum(ask.getQty()));
        }
        StringBuilder bidString = new StringBuilder();
        for (Level bid: bids){
            bidString.append(generateStrForCheckSum(bid.getPrice()));
            bidString.append(generateStrForCheckSum(bid.getQty()));
        }
        String obStr = askStr.toString() + bidString.toString();
        CRC32 crc = new CRC32();
        crc.update(obStr.getBytes());
        return crc.getValue() == checksum;
    }

    public void buildPriceLevelsMap(String symbol, List<Level> priceLevelList, Map<String, Map<BigDecimal, Level>> priceLevels){
        priceLevels.computeIfAbsent(symbol, k -> new HashMap<>())
                .putAll(priceLevelList.stream()
                        .collect(Collectors.toMap(Level::getPrice, level -> level)));
    }

    public void processLevelData(List<Level> data, Map<BigDecimal, Level> priceLevels, TreeSet<Level> levels){
        for (Level level: data){
            Level inMemLevel = priceLevels.get(level.getPrice());
            if (BigDecimal.ZERO.compareTo(level.getQty()) == 0){
                removeLevel(inMemLevel, levels, priceLevels);
            }else{
                insertLevel(inMemLevel, level, levels, priceLevels);
            }
        }
    }

    public void removeLevel(Level level, TreeSet<Level> levels, Map<BigDecimal, Level> priceLevels){
        if (level == null) return;
        levels.remove(level);
        priceLevels.remove(level.getPrice());
    }

    public void insertLevel(Level inMemlevel, Level newLevel, TreeSet<Level> levels, Map<BigDecimal, Level> priceLevels){
        removeLevel(inMemlevel, levels, priceLevels);
        levels.add(newLevel);
        priceLevels.put(newLevel.getPrice(), newLevel);
    }

    public void removeExtraLevels(TreeSet<Level> bids, TreeSet<Level> asks, Map<BigDecimal, Level> priceLevels, int maxDepth){
        Level lastLevel = null;
        while (bids.size() > maxDepth){
            lastLevel = bids.last();
            removeLevel(lastLevel, bids, priceLevels);
        }

        while (asks.size() > maxDepth){
            lastLevel = asks.first();
            removeLevel(lastLevel, asks, priceLevels);
        }
    }


    public void buildCandle(Candle candle, LocalDateTime start, LocalDateTime now, BigDecimal bid, BigDecimal ask){
        BigDecimal value = bid.add(ask).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        if (now.isEqual(start)){
            candle.setOpen(value);
        } else if (start.plusMinutes(1L).isEqual(now) || now.isAfter(start.plusMinutes(1L))) {
            candle.setClose(value);
        }else{
            candle.setHigh(candle.getHigh() == null ? value :value.max(candle.getHigh()));
            candle.setLow(candle.getLow() == null ? value :value.min(candle.getLow()));
        }
        candle.setTicks(candle.getTicks() + 1);
    }

    public boolean isInvalidCandle(Candle candle){
        return Arrays.stream(candle.getClass()
                        .getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .map(f -> CommonUtil.getFieldByFieldName(candle, f.getName()))
                .allMatch(Objects::isNull);
    }
}
