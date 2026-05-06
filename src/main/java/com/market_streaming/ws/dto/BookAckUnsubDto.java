package com.market_streaming.ws.dto;

import lombok.Data;

@Data
public class BookAckUnsubDto {
    private String channel;
    private String symbol;
    private Integer depth;
}
