package com.assignment.ws.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookAckSubscribeDto {
    private String channel;
    private String symbol;
    private List<String> warnings;
    private Integer depth;
    private boolean snapshot;
}
