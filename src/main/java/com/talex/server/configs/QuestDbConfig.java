package com.talex.server.configs;

import io.questdb.client.Sender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuestDbConfig {

    @Value("${questdb.host}")
    private String host;

    @Value("${questdb.port}")
    private int port;

    @Bean(destroyMethod = "close")
    public Sender questDBSender() {
        // Khởi tạo Sender kết nối qua giao thức mạng TCP siêu tốc của QuestDB
        return Sender.builder(Sender.Transport.TCP)
                .address(host + ":" + port)
                .bufferCapacity(1024)
                .build();
    }
}
