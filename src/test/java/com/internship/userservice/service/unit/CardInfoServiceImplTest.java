package com.internship.userservice.service.unit;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;
import com.internship.userservice.entity.CardInfo;
import com.internship.userservice.entity.User;
import com.internship.userservice.exception.AlreadyExistsException;
import com.internship.userservice.exception.NotFoundException;
import com.internship.userservice.mapper.CardInfoMapper;
import com.internship.userservice.repository.CardInfoRepository;
import com.internship.userservice.repository.UserRepository;
import com.internship.userservice.security.JwtUtils;
import com.internship.userservice.service.impl.CardInfoServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CardInfoServiceImplTest {

    private static final Long AUTH_USER_CRED_ID = 100L;

    private CardInfoRepository cardInfoRepository;
    private UserRepository userRepository;
    private CardInfoMapper cardInfoMapper;
    private CardInfoServiceImpl cardInfoService;
    private MockedStatic<JwtUtils> jwtUtilsMock;
    private CacheManager cacheManager;
    private Cache usersCache;
    private Cache usersByEmailCache;


    @BeforeEach
    void setUp() {
        cardInfoRepository = mock(CardInfoRepository.class);
        userRepository = mock(UserRepository.class);
        cardInfoMapper = mock(CardInfoMapper.class);

        cacheManager = mock(CacheManager.class);
        usersCache = mock(Cache.class);
        usersByEmailCache = mock(Cache.class);
        when(cacheManager.getCache("users")).thenReturn(usersCache);
        when(cacheManager.getCache("usersByEmail")).thenReturn(usersByEmailCache);

        cardInfoService = new CardInfoServiceImpl(
                cardInfoRepository, userRepository, cardInfoMapper, cacheManager
        );

        jwtUtilsMock = mockStatic(JwtUtils.class);
        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(AUTH_USER_CRED_ID);
    }

    @AfterEach
    void tearDown() {
        jwtUtilsMock.close();
    }

    @Test
    void create_ShouldCreateCard_WhenDataIsValid() {

        CardInfoRequest request = new CardInfoRequest();
        request.setNumber("1234567890123456");
        request.setHolder("JOHN DOE");
        request.setExpirationDate("12/30");

        User owner = new User();
        owner.setId(1L);
        owner.setUserCredentialsId(AUTH_USER_CRED_ID);

        CardInfo cardToSave = new CardInfo();
        cardToSave.setNumber("1234567890123456");
        cardToSave.setHolder("JOHN DOE");
        cardToSave.setExpirationDate("12/30");

        CardInfo saved = new CardInfo();
        saved.setId(10L);
        saved.setNumber("1234567890123456");
        saved.setHolder("JOHN DOE");
        saved.setExpirationDate("12/30");
        saved.setUser(owner);

        CardInfoResponse expected = new CardInfoResponse();
        expected.setId(10L);
        expected.setNumber("1234567890123456");

        when(userRepository.findByUserCredentialsId(AUTH_USER_CRED_ID)).thenReturn(Optional.of(owner));
        when(cardInfoRepository.existsByNumber("1234567890123456")).thenReturn(false);
        when(cardInfoMapper.toEntity(request)).thenReturn(cardToSave);
        when(cardInfoRepository.save(cardToSave)).thenReturn(saved);
        when(cardInfoMapper.toDto(saved)).thenReturn(expected);

        CardInfoResponse result = cardInfoService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getNumber()).isEqualTo("1234567890123456");

        verify(userRepository).findByUserCredentialsId(AUTH_USER_CRED_ID);
        verify(cardInfoRepository).existsByNumber("1234567890123456");
        verify(cardInfoMapper).toEntity(request);
        verify(cardInfoRepository).save(cardToSave);
        verify(cardInfoMapper).toDto(saved);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void create_ShouldThrowAlreadyExistsException_WhenCardNumberExists() {

        CardInfoRequest request = new CardInfoRequest();
        request.setNumber("1111222233334444");
        request.setHolder("JANE DOE");
        request.setExpirationDate("01/29");

        User owner = new User();
        owner.setId(1L);
        owner.setUserCredentialsId(AUTH_USER_CRED_ID);

        when(userRepository.findByUserCredentialsId(AUTH_USER_CRED_ID)).thenReturn(Optional.of(owner));
        when(cardInfoRepository.existsByNumber("1111222233334444")).thenReturn(true);

        assertThatThrownBy(() -> cardInfoService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Card number '1111222233334444' already exists");

        verify(userRepository).findByUserCredentialsId(AUTH_USER_CRED_ID);
        verify(cardInfoRepository).existsByNumber("1111222233334444");
        verifyNoMoreInteractions(cardInfoMapper, cardInfoRepository);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void create_ShouldThrowNotFoundException_WhenOwnerUserNotFound() {

        CardInfoRequest request = new CardInfoRequest();
        request.setNumber("1111222233334444");
        request.setHolder("JANE DOE");
        request.setExpirationDate("01/29");

        when(userRepository.findByUserCredentialsId(AUTH_USER_CRED_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardInfoService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User with credentials id=" + AUTH_USER_CRED_ID + " not found");

        verify(userRepository).findByUserCredentialsId(AUTH_USER_CRED_ID);
        verifyNoMoreInteractions(cardInfoRepository, cardInfoMapper);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void getCardById_ShouldReturnCard_WhenExists() {

        Long cardId = 100L;

        CardInfo card = new CardInfo();
        card.setId(cardId);
        card.setNumber("1111222233334444");

        CardInfoResponse expected = new CardInfoResponse();
        expected.setId(cardId);
        expected.setNumber("1111222233334444");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardInfoMapper.toDto(card)).thenReturn(expected);

        CardInfoResponse result = cardInfoService.getCardById(cardId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        assertThat(result.getNumber()).isEqualTo("1111222233334444");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoMapper).toDto(card);
    }

    @Test
    void getCardById_ShouldThrowNotFoundException_WhenNotExists() {

        Long cardId = 1488L;
        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardInfoService.getCardById(cardId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=1488 not found");

        verify(cardInfoRepository).findById(cardId);
        verifyNoMoreInteractions(cardInfoMapper);
    }

    @Test
    void getAllByIds_ShouldReturnListOfCards_WhenCardsExist() {

        List<Long> ids = List.of(1L, 2L);

        CardInfo c1 = new CardInfo(); c1.setId(1L); c1.setNumber("1111222233334444");
        CardInfo c2 = new CardInfo(); c2.setId(2L); c2.setNumber("4444333322221111");
        List<CardInfo> cards = List.of(c1, c2);

        CardInfoResponse r1 = new CardInfoResponse(); r1.setId(1L); r1.setNumber("1111222233334444");
        CardInfoResponse r2 = new CardInfoResponse(); r2.setId(2L); r2.setNumber("4444333322221111");

        when(cardInfoRepository.findAllById(ids)).thenReturn(cards);
        when(cardInfoMapper.toDtoList(cards)).thenReturn(List.of(r1, r2));

        List<CardInfoResponse> result = cardInfoService.getAllByIds(ids);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);

        verify(cardInfoRepository).findAllById(ids);
        verify(cardInfoMapper).toDtoList(cards);
    }

    @Test
    void getByUserId_ShouldReturnListOfCards_WhenCardsExist() {

        Long userId = 7L;

        CardInfo c1 = new CardInfo(); c1.setId(10L); c1.setNumber("1111222233334444");
        CardInfo c2 = new CardInfo(); c2.setId(20L); c2.setNumber("4444333322221111");
        List<CardInfo> cards = List.of(c1, c2);

        CardInfoResponse r1 = new CardInfoResponse(); r1.setId(10L); r1.setNumber("1111222233334444");
        CardInfoResponse r2 = new CardInfoResponse(); r2.setId(20L); r2.setNumber("4444333322221111");

        when(cardInfoRepository.findByUserId(userId)).thenReturn(cards);
        when(cardInfoMapper.toDtoList(cards)).thenReturn(List.of(r1, r2));

        List<CardInfoResponse> result = cardInfoService.getByUserId(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(1).getId()).isEqualTo(20L);

        verify(cardInfoRepository).findByUserId(userId);
        verify(cardInfoMapper).toDtoList(cards);
    }

    @Test
    void update_ShouldUpdateCard_WhenOwnerMatches_AndNumberChangesToUnique() {

        Long cardId = 1L;

        User owner = new User();
        owner.setId(10L);
        owner.setUserCredentialsId(AUTH_USER_CRED_ID);

        CardInfo existing = new CardInfo();
        existing.setId(cardId);
        existing.setNumber("1111222233334444");
        existing.setUser(owner);

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("4444333322221111");
        dto.setHolder("JOHN DOE");
        dto.setExpirationDate("01/29");

        CardInfo saved = new CardInfo();
        saved.setId(cardId);
        saved.setNumber("4444333322221111");
        saved.setUser(owner);

        CardInfoResponse resp = new CardInfoResponse();
        resp.setId(cardId);
        resp.setNumber("4444333322221111");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(existing));
        when(cardInfoRepository.existsByNumber(dto.getNumber())).thenReturn(false);
        when(cardInfoRepository.save(existing)).thenReturn(saved);
        when(cardInfoMapper.toDto(saved)).thenReturn(resp);

        CardInfoResponse result = cardInfoService.update(cardId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getNumber()).isEqualTo("4444333322221111");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository).existsByNumber(dto.getNumber());
        verify(cardInfoMapper).updateEntity(existing, dto);
        verify(cardInfoRepository).save(existing);
        verify(cardInfoMapper).toDto(saved);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void update_ShouldUpdate_WhenNumberNotChanged() {

        Long cardId = 1L;

        User owner = new User();
        owner.setId(10L);
        owner.setUserCredentialsId(AUTH_USER_CRED_ID);

        CardInfo existing = new CardInfo();
        existing.setId(cardId);
        existing.setNumber("1111222233334444");
        existing.setUser(owner);

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("1111222233334444");
        dto.setHolder("X");
        dto.setExpirationDate("12/30");

        CardInfo saved = new CardInfo();
        saved.setId(cardId);
        saved.setNumber("1111222233334444");
        saved.setUser(owner);

        CardInfoResponse resp = new CardInfoResponse();
        resp.setId(cardId);
        resp.setNumber("1111222233334444");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(existing));
        when(cardInfoRepository.save(existing)).thenReturn(saved);
        when(cardInfoMapper.toDto(saved)).thenReturn(resp);

        CardInfoResponse result = cardInfoService.update(cardId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);

        verify(cardInfoRepository).findById(cardId);

        verify(cardInfoRepository, never()).existsByNumber(any());
        verify(cardInfoMapper).updateEntity(existing, dto);
        verify(cardInfoRepository).save(existing);
        verify(cardInfoMapper).toDto(saved);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void update_ShouldThrowAlreadyExists_WhenNewNumberAlreadyExists() {

        Long cardId = 1L;

        User owner = new User();
        owner.setId(10L);
        owner.setUserCredentialsId(AUTH_USER_CRED_ID);

        CardInfo existing = new CardInfo();
        existing.setId(cardId);
        existing.setNumber("1111222233334444");
        existing.setUser(owner);

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("4444333322221111");
        dto.setHolder("HOLDER");
        dto.setExpirationDate("01/29");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(existing));
        when(cardInfoRepository.existsByNumber("4444333322221111")).thenReturn(true);

        assertThatThrownBy(() -> cardInfoService.update(cardId, dto))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Card number '4444333322221111' already exists");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository).existsByNumber("4444333322221111");
        verify(cardInfoRepository, never()).save(any());
        verifyNoMoreInteractions(cardInfoMapper);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void update_ShouldThrowAccessDenied_WhenOwnerMismatch() {

        Long cardId = 1L;

        User owner = new User();
        owner.setId(10L);
        owner.setUserCredentialsId(777L);

        CardInfo existing = new CardInfo();
        existing.setId(cardId);
        existing.setNumber("1111222233334444");
        existing.setUser(owner);

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("4444333322221111");
        dto.setHolder("HOLDER");
        dto.setExpirationDate("01/29");

        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(999L);
        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> cardInfoService.update(cardId, dto))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository, never()).existsByNumber(any());
        verify(cardInfoRepository, never()).save(any());
        verifyNoMoreInteractions(cardInfoMapper);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void update_ShouldThrowNotFound_WhenCardMissing() {

        Long cardId = 999L;

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("4444333322221111");
        dto.setHolder("H");
        dto.setExpirationDate("01/29");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardInfoService.update(cardId, dto))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=999 not found");

        verify(cardInfoRepository).findById(cardId);
        verifyNoMoreInteractions(cardInfoRepository, cardInfoMapper);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void delete_ShouldDelete_WhenOwnerMatches() {

        Long cardId = 1L;

        User owner = new User();
        owner.setId(10L);
        owner.setUserCredentialsId(AUTH_USER_CRED_ID);

        CardInfo card = new CardInfo();
        card.setId(cardId);
        card.setUser(owner);

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardInfoService.delete(cardId);

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository).deleteById(cardId);
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void delete_ShouldThrowNotFound_WhenCardMissing() {

        Long cardId = 1488L;
        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardInfoService.delete(cardId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=1488 not found");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository, never()).deleteById(any());
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }

    @Test
    void delete_ShouldThrowAccessDenied_WhenOwnerMismatch() {
        Long cardId = 1L;

        User owner = new User();
        owner.setId(10L);
        owner.setUserCredentialsId(777L);

        CardInfo card = new CardInfo();
        card.setId(cardId);
        card.setUser(owner);

        jwtUtilsMock.when(JwtUtils::getUserCredentialsIdFromToken).thenReturn(999L);
        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardInfoService.delete(cardId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("Access denied");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository, never()).deleteById(any());
        jwtUtilsMock.verify(JwtUtils::getUserCredentialsIdFromToken);
    }
}

