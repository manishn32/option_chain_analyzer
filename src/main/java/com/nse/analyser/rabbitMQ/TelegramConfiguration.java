package com.nse.analyser.rabbitMQ;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramConfiguration {
    private String info;
    private String errors;
}
