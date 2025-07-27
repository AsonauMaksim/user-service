package com.internship.userservice.service.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@TestPropertySource(locations = "classpath:application-test.yml")
public abstract class BaseIntegrationTest {

    static boolean isCi = System.getenv("GITHUB_ACTIONS") != null;

    @Container
    static final PostgreSQLContainer<?> postgres =
            !isCi ? new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("user_db_test")
                    .withUsername("test_user")
                    .withPassword("test_password") : null;

    @Container
    static final GenericContainer<?> redis =
            !isCi ? new GenericContainer<>("redis:7.2")
                    .withExposedPorts(6379) : null;

    static {
        if (!isCi) {
            postgres.start();
            redis.start();
            System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
            System.setProperty("spring.datasource.username", postgres.getUsername());
            System.setProperty("spring.datasource.password", postgres.getPassword());
            System.setProperty("spring.redis.host", redis.getHost());
            System.setProperty("spring.redis.port", String.valueOf(redis.getFirstMappedPort()));
        }
    }
}
