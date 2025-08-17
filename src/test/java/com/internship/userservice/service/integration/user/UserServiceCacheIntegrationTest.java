package com.internship.userservice.service.integration.user;

import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.service.UserService;
import com.internship.userservice.service.integration.BaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserServiceCacheIntegrationTest extends BaseIntegrationTest {

    private static final long OWNER_AUTH_ID = 100L;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    private static final String USERS_CACHE = "users";

    private void authenticateAs(long userCredentialsId) {
        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(userCredentialsId), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void clearCache() {
        Cache cache = cacheManager.getCache(USERS_CACHE);
        if (cache != null) cache.clear();
        Cache byEmail = cacheManager.getCache("usersByEmail");
        if (byEmail != null) byEmail.clear();
    }

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserById_ShouldCacheResult() {

        authenticateAs(OWNER_AUTH_ID);

        UserRequest request = createUserRequest();
        UserResponse savedUser = userService.create(request);
        Long userId = savedUser.getId();

        userService.getUserById(userId);

        Cache.ValueWrapper cached =
                Objects.requireNonNull(cacheManager.getCache(USERS_CACHE)).get(userId);

        assertThat(cached).isNotNull();
        assertThat(((UserResponse) Objects.requireNonNull(cached.get())).getEmail())
                .isEqualTo("max@gmail.com");
    }

    @Test
    void updateUserById_ShouldUpdateCache() {

        authenticateAs(OWNER_AUTH_ID);

        UserResponse savedUser = userService.create(createUserRequest());
        Long userId = savedUser.getId();

        userService.getUserById(userId);
        Cache.ValueWrapper cachedBeforeUpdate =
                Objects.requireNonNull(cacheManager.getCache(USERS_CACHE)).get(userId);
        assertThat(cachedBeforeUpdate).isNotNull();
        assertThat(((UserResponse) Objects.requireNonNull(cachedBeforeUpdate.get())).getName())
                .isEqualTo("Max");

        authenticateAs(OWNER_AUTH_ID);
        UserRequest update = UserRequest.builder()
                .name("Updated")
                .surname("User")
                .birthDate(LocalDate.of(1990, 1, 1))
                .email("max@gmail.com")
                .build();
        userService.updateUserById(userId, update);

        Cache.ValueWrapper cachedAfterUpdate =
                Objects.requireNonNull(cacheManager.getCache(USERS_CACHE)).get(userId);
        assertThat(cachedAfterUpdate).isNotNull();
        UserResponse cachedUser = (UserResponse) cachedAfterUpdate.get();
        assert cachedUser != null;
        assertThat(cachedUser.getName()).isEqualTo("Updated");
        assertThat(cachedUser.getSurname()).isEqualTo("User");
        assertThat(cachedUser.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    void deleteUserById_ShouldEvictCache() {
        authenticateAs(OWNER_AUTH_ID);

        UserResponse savedUser = userService.create(createUserRequest());
        Long userId = savedUser.getId();

        userService.getUserById(userId);
        assertThat(Objects.requireNonNull(cacheManager.getCache(USERS_CACHE)).get(userId))
                .isNotNull();

        authenticateAs(OWNER_AUTH_ID);
        userService.deleteUserById(userId);

        assertThat(Objects.requireNonNull(cacheManager.getCache(USERS_CACHE)).get(userId))
                .isNull();
    }

    @Test
    void getUserByEmail_ShouldCacheResult() {

        authenticateAs(OWNER_AUTH_ID);

        userService.create(createUserRequest());

        userService.getUserByEmail("max@gmail.com");

        Cache.ValueWrapper cached =
                Objects.requireNonNull(cacheManager.getCache("usersByEmail")).get("max@gmail.com");

        assertThat(cached).isNotNull();
        UserResponse cachedUser = (UserResponse) Objects.requireNonNull(cached.get());
        assertThat(cachedUser.getEmail()).isEqualTo("max@gmail.com");
    }

    private UserRequest createUserRequest() {

        return UserRequest.builder()
                .name("Max")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();
    }
}
