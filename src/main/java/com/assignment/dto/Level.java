package com.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Level implements Comparable<Level>{
    private BigDecimal price;
    private BigDecimal qty;

    @Override
    public int compareTo(Level o) {
        return o.price.compareTo(this.price);
    }

    @Override
    public String toString() {
        return "{price:" + price + ",qty:" + qty +'}';
    }
}
