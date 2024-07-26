package com.assignment.ws.dto;

import com.assignment.dto.Level;
import lombok.Builder;
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
