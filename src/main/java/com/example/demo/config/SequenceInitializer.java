package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SequenceInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            Long maxUserId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM users", Long.class);
            if (maxUserId != null && maxUserId > 0) {
                jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH " + (maxUserId + 1));
            }
            
            Long maxCategoryId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM categories", Long.class);
            if (maxCategoryId != null && maxCategoryId > 0) {
                jdbcTemplate.execute("ALTER TABLE categories ALTER COLUMN id RESTART WITH " + (maxCategoryId + 1));
            }
        } catch (Exception e) {
        }
    }
}

