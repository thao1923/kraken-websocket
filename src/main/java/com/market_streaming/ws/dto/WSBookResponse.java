package com.market_streaming.ws.dto;

import lombok.Data;

import java.util.List;

@Data
public class WSBookResponse {
    private String channel;
    private String type;
    private List<BookData> data;

}
