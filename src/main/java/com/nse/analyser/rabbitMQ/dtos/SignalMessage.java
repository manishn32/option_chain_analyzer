package com.nse.analyser.rabbitMQ.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalMessage {

    private String symbol;
    private String sentiment;
    private Double pcr;
    private Long timestamp;
}