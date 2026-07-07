package com.talex.server.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ua_parser.Parser;

@Configuration
public class UserAgentConfig {

    @Bean
    public Parser uaParser() {
        return new Parser();
    }
}