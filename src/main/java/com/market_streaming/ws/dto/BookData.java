package com.market_streaming.ws.dto;

import com.market_streaming.dto.Level;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class BookData {
    private String symbol;
    private List<Level> bids;
    private List<Level> asks;
    private Long checksum;
    private String timestamp;
}
