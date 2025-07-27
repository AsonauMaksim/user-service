package com.internship.userservice.service.integration;

import com.internship.userservice.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16";
    private static final String REDIS_IMAGE = "redis:7.2";

    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("user_db_test")
                    .withUsername("postgres")
                    .withPassword("12345");

    protected static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
                    .withExposedPorts(6379);

    static {
        POSTGRES_CONTAINER.start();
        REDIS_CONTAINER.start();
    }

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(name -> {
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        });
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);

        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }
}