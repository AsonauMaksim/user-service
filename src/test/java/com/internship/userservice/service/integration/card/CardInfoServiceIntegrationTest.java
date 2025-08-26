package com.internship.userservice.service.integration.card;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;
import com.internship.userservice.dto.user.UserRequest;
import com.internship.userservice.dto.user.UserResponse;
import com.internship.userservice.exception.AlreadyExistsException;
import com.internship.userservice.exception.NotFoundException;
import com.internship.userservice.repository.CardInfoRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql(scripts = "classpath:/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class CardInfoServiceIntegrationTest extends BaseIntegrationTest {

    private static final long OWNER_AUTH_ID = 100L;
    private static final long OTHER_AUTH_ID = 200L;

    @Autowired
    private CardInfoService cardInfoService;

    @Autowired
    private UserService userService;

    @Autowired
    private CardInfoRepository cardInfoRepository;

    @Autowired
    private CacheManager cacheManager;

    private UserResponse savedUser;

    @BeforeEach
    void clearCaches() {
        Cache cards = cacheManager.getCache("cards");
        if (cards != null) cards.clear();
    }

    @BeforeEach
    void initUser() {
        UserRequest user = UserRequest.builder()
                .name("Max")
                .surname("Ivanov")
                .birthDate(LocalDate.of(1995, 10, 17))
                .email("max@gmail.com")
                .build();
        savedUser = userService.create(user, OWNER_AUTH_ID);
    }

    @Test
    void create_ShouldSaveCard() {
        CardInfoRequest request = CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();

        CardInfoResponse response = cardInfoService.create(request, OWNER_AUTH_ID);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getNumber()).isEqualTo("1111222233334444");
        assertThat(response.getHolder()).isEqualTo("Max Ivanov");
        assertThat(response.getExpirationDate()).isEqualTo("01/30");
    }

    @Test
    void create_ShouldThrow_WhenCardNumberAlreadyExists() {
        CardInfoRequest request = CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();

        cardInfoService.create(request, OWNER_AUTH_ID);

        assertThatThrownBy(() -> cardInfoService.create(request, OWNER_AUTH_ID))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Card number '1111222233334444' already exists");
    }

    @Test
    void create_ShouldThrow_WhenOwnerUserNotFound() {
        CardInfoRequest request = CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Ghost Owner")
                .expirationDate("01/30")
                .build();

        assertThatThrownBy(() -> cardInfoService.create(request, 9999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with credentials id=9999 not found");
    }

    @Test
    void getCardById_ShouldReturnCard() {
        CardInfoRequest request = CardInfoRequest.builder()
                .number("9999888877776666")
                .holder("Max Ivanov")
                .expirationDate("12/30")
                .build();

        CardInfoResponse savedCard = cardInfoService.create(request, OWNER_AUTH_ID);

        CardInfoResponse foundCard = cardInfoService.getCardById(savedCard.getId());

        assertThat(foundCard.getId()).isEqualTo(savedCard.getId());
        assertThat(foundCard.getNumber()).isEqualTo("9999888877776666");
        assertThat(foundCard.getHolder()).isEqualTo("Max Ivanov");
        assertThat(foundCard.getExpirationDate()).isEqualTo("12/30");
    }

    @Test
    void getCardById_ShouldThrow_WhenNotFound() {
        Long nonExistentId = 1488L;

        assertThatThrownBy(() -> cardInfoService.getCardById(nonExistentId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=1488 not found");
    }

    @Test
    void getAllByIds_ShouldReturnCards() {
        CardInfoResponse card1 = cardInfoService.create(CardInfoRequest.builder()
                .number("1111222233330001")
                .holder("Ivan Ivanov")
                .expirationDate("01/30")
                .build(), OWNER_AUTH_ID);

        CardInfoResponse card2 = cardInfoService.create(CardInfoRequest.builder()
                .number("1111222233330002")
                .holder("Sveta Svetikova")
                .expirationDate("02/30")
                .build(), OWNER_AUTH_ID);

        List<CardInfoResponse> cards = cardInfoService.getAllByIds(List.of(card1.getId(), card2.getId()));

        assertThat(cards).hasSize(2);
        assertThat(cards)
                .extracting(CardInfoResponse::getNumber)
                .containsExactlyInAnyOrder("1111222233330001", "1111222233330002");
    }

    @Test
    void getAllByIds_ShouldReturnEmptyList_WhenIdsEmpty() {
        List<CardInfoResponse> cards = cardInfoService.getAllByIds(List.of());
        assertThat(cards).isEmpty();
    }

    @Test
    void getByUserId_ShouldReturnCardsForUser() {
        cardInfoService.create(CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Max Ivanov")
                .expirationDate("05/30")
                .build(), OWNER_AUTH_ID);

        cardInfoService.create(CardInfoRequest.builder()
                .number("4444333322221111")
                .holder("Max Ivanov")
                .expirationDate("06/30")
                .build(), OWNER_AUTH_ID);

        List<CardInfoResponse> cards = cardInfoService.getByUserId(savedUser.getId());

        assertThat(cards).hasSize(2);
        assertThat(cards).allMatch(card -> card.getHolder().equals("Max Ivanov"));
    }

    @Test
    void getByUserId_ShouldReturnEmptyList_WhenUserHasNoCards() {
        List<CardInfoResponse> cards = cardInfoService.getByUserId(savedUser.getId());
        assertThat(cards).isEmpty();
    }

    @Test
    void update_ShouldUpdateCardSuccessfully() {
        CardInfoResponse saved = cardInfoService.create(CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build(), OWNER_AUTH_ID);

        CardInfoRequest update = CardInfoRequest.builder()
                .number("1111222233334444")
                .holder("Sveta Svetikova")
                .expirationDate("12/30")
                .build();

        CardInfoResponse updated = cardInfoService.update(saved.getId(), update, OWNER_AUTH_ID);

        assertThat(updated.getHolder()).isEqualTo("Sveta Svetikova");
        assertThat(updated.getExpirationDate()).isEqualTo("12/30");
    }

    @Test
    void update_ShouldThrowAlreadyExistsException_WhenCardNumberExists() {
        cardInfoService.create(CardInfoRequest.builder()
                .number("1111222233330000")
                .holder("First")
                .expirationDate("01/30")
                .build(), OWNER_AUTH_ID);

        CardInfoResponse cardToUpdate = cardInfoService.create(CardInfoRequest.builder()
                .number("9999888877770000")
                .holder("Second")
                .expirationDate("01/30")
                .build(), OWNER_AUTH_ID);

        CardInfoRequest update = CardInfoRequest.builder()
                .number("1111222233330000")
                .holder("New Holder")
                .expirationDate("12/30")
                .build();

        assertThatThrownBy(() -> cardInfoService.update(cardToUpdate.getId(), update, OWNER_AUTH_ID))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenOwnerMismatch() {
        CardInfoResponse saved = cardInfoService.create(CardInfoRequest.builder()
                .number("1234123412341234")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build(), OWNER_AUTH_ID);

        CardInfoRequest update = CardInfoRequest.builder()
                .number("1234123412341234")
                .holder("Hacker")
                .expirationDate("02/31")
                .build();

        assertThatThrownBy(() -> cardInfoService.update(saved.getId(), update, OTHER_AUTH_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void update_ShouldThrowNotFoundException_WhenCardNotFound() {
        CardInfoRequest update = CardInfoRequest.builder()
                .number("1111222233330000")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build();

        assertThatThrownBy(() -> cardInfoService.update(1488L, update, OWNER_AUTH_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=1488 not found");
    }

    @Test
    void delete_ShouldRemoveCardSuccessfully() {
        CardInfoResponse saved = cardInfoService.create(CardInfoRequest.builder()
                .number("5555666677778888")
                .holder("Max Ivanov")
                .expirationDate("01/30")
                .build(), OWNER_AUTH_ID);

        Long cardId = saved.getId();

        assertThat(cardInfoRepository.existsById(cardId)).isTrue();

        cardInfoService.delete(cardId, OWNER_AUTH_ID);

        assertThat(cardInfoRepository.existsById(cardId)).isFalse();
    }

    @Test
    void delete_ShouldThrowNotFoundException_WhenCardDoesNotExist() {
        assertThatThrownBy(() -> cardInfoService.delete(1488L, OWNER_AUTH_ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=1488 not found");
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenOwnerMismatch() {
        CardInfoResponse saved = cardInfoService.create(CardInfoRequest.builder()
                .number("1010101010101010")
                .holder("Max Ivanov")
                .expirationDate("10/30")
                .build(), OWNER_AUTH_ID);

        assertThatThrownBy(() -> cardInfoService.delete(saved.getId(), OTHER_AUTH_ID))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }
}
