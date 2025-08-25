package com.internship.userservice.service.integration.card;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;
import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.service.CardInfoService;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CardInfoServiceCacheIntegrationTest extends BaseIntegrationTest {

    private static final long OWNER_AUTH_ID = 200L;

    @Autowired
    private CardInfoService cardInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    private static final String CARDS_CACHE = "cards";

    private void authenticateAs(long userCredentialsId) {

        var auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(userCredentialsId), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @BeforeEach
    void setUp() {

        clearCache();
        authenticateAs(OWNER_AUTH_ID);

        UserRequest user = UserRequest.builder()
                .name("Max")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();
        userService.create(user);
    }

    @AfterEach
    void clearCtx() {

        SecurityContextHolder.clearContext();
    }

    private void clearCache() {

        Cache cache = cacheManager.getCache(CARDS_CACHE);
        if (cache != null) cache.clear();
    }

    @Test
    void getCardById_ShouldCacheResult() {

        CardInfoRequest request = CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();

        CardInfoResponse savedCard = cardInfoService.create(request);
        cardInfoService.getCardById(savedCard.getId());

        Cache.ValueWrapper cached =
                Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(savedCard.getId());

        assertThat(cached).isNotNull();
        assertThat(((CardInfoResponse) Objects.requireNonNull(cached.get())).getNumber())
                .isEqualTo("1111222233334444");
    }

    @Test
    void update_ShouldUpdateCache() {

        CardInfoRequest createRequest = CardInfoRequest.builder()
                .number("2222333344445555")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();
        CardInfoResponse savedCard = cardInfoService.create(createRequest);

        cardInfoService.getCardById(savedCard.getId());
        Cache.ValueWrapper beforeUpdate =
                Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(savedCard.getId());
        assertThat(beforeUpdate).isNotNull();
        assertThat(((CardInfoResponse) Objects.requireNonNull(beforeUpdate.get())).getHolder())
                .isEqualTo("Max Ivanov");

        CardInfoRequest updateRequest = CardInfoRequest.builder()
                .number("2222333344445555")
                .holder("Sveta Svetikova")
                .expirationDate("12/30")
                .build();
        cardInfoService.update(savedCard.getId(), updateRequest);

        Cache.ValueWrapper afterUpdate =
                Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(savedCard.getId());
        assertThat(afterUpdate).isNotNull();

        CardInfoResponse updated = (CardInfoResponse) afterUpdate.get();
        assertThat(updated).isNotNull();
        assertThat(updated.getHolder()).isEqualTo("Sveta Svetikova");
        assertThat(updated.getExpirationDate()).isEqualTo("12/30");
    }

    @Test
    void delete_ShouldEvictCache() {

        CardInfoRequest request = CardInfoRequest.builder()
                .number("1234432112344321")
                .holder("Max Ivanov")
                .expirationDate("11/30")
                .build();

        CardInfoResponse savedCard = cardInfoService.create(request);
        Long cardId = savedCard.getId();

        cardInfoService.getCardById(cardId);

        assertThat(Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(cardId))
                .isNotNull();

        cardInfoService.delete(cardId);

        assertThat(Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(cardId))
                .isNull();
    }
}
