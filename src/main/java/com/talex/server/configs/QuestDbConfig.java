package com.talex.server.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class QuestDbConfig {

    @Value("${questdb.host}")
    private String host;

    @Value("${questdb.port}")
    private int port;

    @Value("${questdb.username}")
    private String username;

    @Value("${questdb.password}")
    private String password;

    @Bean(name = "questDbJdbcTemplate")
    public JdbcTemplate questDbJdbcTemplate() {
        var dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/qdb");
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }
}
