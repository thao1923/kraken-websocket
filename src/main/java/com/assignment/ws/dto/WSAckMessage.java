package com.assignment.ws.dto;

import lombok.Data;

@Data
public class WSAckMessage<T> {
    private String method;
    private Integer req_id;
    private T result;
    private boolean success;
    private String error;
    private String time_in;
    private String time_out;


}
