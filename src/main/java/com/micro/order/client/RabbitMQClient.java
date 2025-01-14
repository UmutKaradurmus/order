package com.micro.order.client;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQClient {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQClient(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public String sendAndReceive(String queue, String message) {
        return (String) rabbitTemplate.convertSendAndReceive(queue, message);
    }
}
