package com.talex.server.configs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentSequenceInitializer implements CommandLineRunner {

    private final JdbcTemplate postgresJdbcTemplate;

    @Override
    public void run(String... args) {
        postgresJdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS payment_code_seq START 100000");
        log.info("payment_code_seq ready");
    }
}
