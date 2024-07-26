package com.assignment.ws.dto;

import lombok.Data;

import java.util.List;

@Data
public class BookRequestParams {
    private String channel;
    private List<String> symbol;
    private Integer depth;
    private Boolean snapshot;
}
