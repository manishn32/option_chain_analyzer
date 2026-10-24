package com.nse.analyser.rabbitMQ.producer;



import com.nse.analyser.rabbitMQ.TelegramConfiguration;
import com.nse.analyser.rabbitMQ.config.RabbitConfig;
import com.nse.analyser.rabbitMQ.dtos.Communications;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramPublisher {
    private final TelegramConfiguration telegramConfiguration;
    private final RabbitTemplate rabbitTemplate;

    public void publish(Communications message) {
        message.setChatId(telegramConfiguration.getInfo());
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                message);
    }
}
