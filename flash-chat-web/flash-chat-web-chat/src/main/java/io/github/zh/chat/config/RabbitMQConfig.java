package io.github.zh.chat.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class RabbitMQConfig {

    public static final String CHAT_EXCHANGE = "flash.chat.fanout";

    @Bean
    public FanoutExchange chatExchange() {
        return new FanoutExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatQueue() {
        return new Queue("flash.chat.queue." + UUID.randomUUID().toString().substring(0, 8),
                false, false, true);
    }

    @Bean
    public Binding binding(Queue chatQueue, FanoutExchange chatExchange) {
        return BindingBuilder.bind(chatQueue).to(chatExchange);
    }
}
