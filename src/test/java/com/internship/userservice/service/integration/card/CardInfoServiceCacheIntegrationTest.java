package com.internship.userservice.service.integration.card;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;
import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.service.CardInfoService;
import com.internship.userservice.service.UserService;
import com.internship.userservice.service.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CardInfoServiceCacheIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CardInfoService cardInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    private static final String CARDS_CACHE = "cards";

    private UserResponse savedUser;

    @BeforeEach
    void init() {
        clearRedisCache();

        UserRequest user = UserRequest.builder()
                .name("Max")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();
        savedUser = userService.create(user);
    }

    private void clearRedisCache() {
        Cache cache = cacheManager.getCache(CARDS_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void getCardById_ShouldCacheResult() {
        CardInfoRequest request = CardInfoRequest.builder()
                .userId(savedUser.getId())
                .number("1111222233334444")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();

        CardInfoResponse savedCard = cardInfoService.create(request);
        cardInfoService.getCardById(savedCard.getId());

        Cache.ValueWrapper cached = Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE))
                .get(savedCard.getId());

        assertThat(cached).isNotNull();
        assert cached != null;
        CardInfoResponse cachedCard = (CardInfoResponse) cached.get();
        assert cachedCard != null;
        assertThat(cachedCard.getNumber()).isEqualTo("1111222233334444");
    }

    @Test
    void update_ShouldUpdateCache() {

        CardInfoRequest createRequest = CardInfoRequest.builder()
                .userId(savedUser.getId())
                .number("2222333344445555")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();
        CardInfoResponse savedCard = cardInfoService.create(createRequest);

        cardInfoService.getCardById(savedCard.getId());
        Cache.ValueWrapper beforeUpdate = Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE))
                .get(savedCard.getId());
        assertThat(beforeUpdate).isNotNull();
        assert beforeUpdate != null;
        assertThat(((CardInfoResponse) Objects.requireNonNull(beforeUpdate.get())).getHolder()).isEqualTo("Max Ivanov");

        CardInfoRequest updateRequest = CardInfoRequest.builder()
                .userId(savedUser.getId())
                .number("2222333344445555")
                .holder("Sveta Svetikova")
                .expirationDate("12/30")
                .build();
        cardInfoService.update(savedCard.getId(), updateRequest);

        Cache.ValueWrapper afterUpdate = Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE))
                .get(savedCard.getId());
        assertThat(afterUpdate).isNotNull();

        assert afterUpdate != null;
        CardInfoResponse updated = (CardInfoResponse) afterUpdate.get();
        assert updated != null;
        assertThat(updated.getHolder()).isEqualTo("Sveta Svetikova");
        assertThat(updated.getExpirationDate()).isEqualTo("12/30");
    }

    @Test
    void delete_ShouldEvictCache() {

        CardInfoRequest request = CardInfoRequest.builder()
                .userId(savedUser.getId())
                .number("1234432112344321")
                .holder("Max Ivanov")
                .expirationDate("11/30")
                .build();

        CardInfoResponse savedCard = cardInfoService.create(request);
        Long cardId = savedCard.getId();

        cardInfoService.getCardById(cardId);

        Cache.ValueWrapper cachedBeforeDelete = Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(cardId);
        assertThat(cachedBeforeDelete).isNotNull();

        cardInfoService.delete(cardId);

        Cache.ValueWrapper cachedAfterDelete = Objects.requireNonNull(cacheManager.getCache(CARDS_CACHE)).get(cardId);
        assertThat(cachedAfterDelete).isNull();
    }
}
