package com.assignment.ws.dto;

import lombok.Data;

@Data
public class WSRequestMessage<T> {
    private String method;
    private int req_id;
    private T params;

}
