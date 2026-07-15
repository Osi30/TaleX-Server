package com.talex.server.configs.database;

import io.questdb.client.Sender;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class QuestDBConfig {

    @Value("${questdb.host}")
    private String host;

    @Value("${questdb.ilp-port}")
    private int ilpPort;

    @Bean(destroyMethod = "close")
    public Sender questDBSender() {
        // ILP (Influx Line Protocol) TCP dung port rieng (mac dinh 9009), khac voi
        // port PGwire (8812) dung cho JDBC - 2 giao thuc khong tuong thich port cho nhau
        return Sender.builder(Sender.Transport.TCP)
                .address(host + ":" + ilpPort)
                .bufferCapacity(1024)
                .build();
    }

    @Bean(name = "questDbDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.questdb")
    public DataSource questDbDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "questDbJdbcTemplate")
    public JdbcTemplate questDbJdbcTemplate(@Qualifier("questDbDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
