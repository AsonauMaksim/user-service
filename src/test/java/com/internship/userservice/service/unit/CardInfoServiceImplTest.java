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
import com.internship.userservice.service.impl.CardInfoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CardInfoServiceImplTest {

    private CardInfoRepository cardInfoRepository;
    private UserRepository userRepository;
    private CardInfoMapper cardInfoMapper;
    private CardInfoServiceImpl cardInfoService;

    @BeforeEach
    void setUp() {
        cardInfoRepository = mock(CardInfoRepository.class);
        userRepository = mock(UserRepository.class);
        cardInfoMapper = mock(CardInfoMapper.class);
        cardInfoService = new CardInfoServiceImpl(cardInfoRepository, userRepository, cardInfoMapper);
    }

    @Test
    void create_ShouldCreateCard_WhenDataIsValid() {

        CardInfoRequest request = new CardInfoRequest();
        request.setNumber("1234567890123456");
        request.setUserId(1L);

        User user = new User();
        user.setId(1L);

        CardInfo cardToSave = new CardInfo();
        cardToSave.setNumber("1234567890123456");

        CardInfo savedCard = new CardInfo();
        savedCard.setId(10L);
        savedCard.setNumber("1234567890123456");
        savedCard.setUser(user);

        CardInfoResponse expectedResponse = new CardInfoResponse();
        expectedResponse.setId(10L);
        expectedResponse.setNumber("1234567890123456");

        when(cardInfoRepository.existsByNumber("1234567890123456")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardInfoMapper.toEntity(request)).thenReturn(cardToSave);
        when(cardInfoRepository.save(cardToSave)).thenReturn(savedCard);
        when(cardInfoMapper.toDto(savedCard)).thenReturn(expectedResponse);

        CardInfoResponse result = cardInfoService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getNumber()).isEqualTo("1234567890123456");

        verify(cardInfoRepository).existsByNumber("1234567890123456");
        verify(userRepository).findById(1L);
        verify(cardInfoMapper).toEntity(request);
        verify(cardInfoRepository).save(cardToSave);
        verify(cardInfoMapper).toDto(savedCard);
    }

    @Test
    void create_ShouldThrowAlreadyExistsException_WhenCardNumberExists() {

        CardInfoRequest request = new CardInfoRequest();
        request.setNumber("1111222233334444");

        when(cardInfoRepository.existsByNumber("1111222233334444")).thenReturn(true);

        assertThatThrownBy(() -> cardInfoService.create(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("Card number '1111222233334444' already exists");

        verify(cardInfoRepository).existsByNumber("1111222233334444");
        verifyNoMoreInteractions(userRepository, cardInfoMapper, cardInfoRepository);
    }

    @Test
    void create_ShouldThrowNotFoundException_WhenUserDoesNotExist() {

        CardInfoRequest request = new CardInfoRequest();
        request.setNumber("1111222233334444");
        request.setUserId(228L);

        when(cardInfoRepository.existsByNumber("1111222233334444")).thenReturn(false);
        when(userRepository.findById(228L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardInfoService.create(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User id=228 not found");

        verify(cardInfoRepository).existsByNumber("1111222233334444");
        verify(userRepository).findById(228L);
        verifyNoMoreInteractions(cardInfoMapper, cardInfoRepository);
    }

    @Test
    void getCardById_ShouldReturnCard_WhenExists() {

        Long cardId = 100L;

        CardInfo card = new CardInfo();
        card.setId(cardId);
        card.setNumber("1111222233334444");

        CardInfoResponse expectedResponse = new CardInfoResponse();
        expectedResponse.setId(cardId);
        expectedResponse.setNumber("1111222233334444");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardInfoMapper.toDto(card)).thenReturn(expectedResponse);

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

        CardInfo card1 = new CardInfo();
        card1.setId(1L);
        card1.setNumber("1111222233334444");

        CardInfo card2 = new CardInfo();
        card2.setId(2L);
        card2.setNumber("4444333322221111");

        List<CardInfo> cards = List.of(card1, card2);

        CardInfoResponse response1 = new CardInfoResponse();
        response1.setId(1L);
        response1.setNumber("1111222233334444");

        CardInfoResponse response2 = new CardInfoResponse();
        response2.setId(2L);
        response2.setNumber("4444333322221111");

        when(cardInfoRepository.findAllById(ids)).thenReturn(cards);
        when(cardInfoMapper.toDtoList(cards)).thenReturn(List.of(response1, response2));

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

        CardInfo card1 = new CardInfo();
        card1.setId(10L);
        card1.setNumber("1111222233334444");

        CardInfo card2 = new CardInfo();
        card2.setId(20L);
        card2.setNumber("4444333322221111");

        List<CardInfo> cards = List.of(card1, card2);

        CardInfoResponse response1 = new CardInfoResponse();
        response1.setId(10L);
        response1.setNumber("1111222233334444");

        CardInfoResponse response2 = new CardInfoResponse();
        response2.setId(20L);
        response2.setNumber("4444333322221111");

        when(cardInfoRepository.findByUserId(userId)).thenReturn(cards);
        when(cardInfoMapper.toDtoList(cards)).thenReturn(List.of(response1, response2));

        List<CardInfoResponse> result = cardInfoService.getByUserId(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(1).getId()).isEqualTo(20L);

        verify(cardInfoRepository).findByUserId(userId);
        verify(cardInfoMapper).toDtoList(cards);
    }

    @Test
    void update_ShouldUpdateCard_WhenDataIsValid() {

        Long cardId = 1L;

        CardInfo existingCard = new CardInfo();
        existingCard.setId(cardId);
        existingCard.setNumber("1111222233334444");

        User oldUser = new User();
        oldUser.setId(10L);
        existingCard.setUser(oldUser);

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("4444333322221111");
        dto.setUserId(20L);

        User newUser = new User();
        newUser.setId(20L);

        CardInfo updatedCard = new CardInfo();
        updatedCard.setId(cardId);
        updatedCard.setNumber("4444333322221111");
        updatedCard.setUser(newUser);

        CardInfoResponse response = new CardInfoResponse();
        response.setId(cardId);
        response.setNumber("4444333322221111");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardInfoRepository.existsByNumber(dto.getNumber())).thenReturn(false);
        when(userRepository.findById(dto.getUserId())).thenReturn(Optional.of(newUser));
        when(cardInfoRepository.save(existingCard)).thenReturn(updatedCard);
        when(cardInfoMapper.toDto(updatedCard)).thenReturn(response);

        CardInfoResponse result = cardInfoService.update(cardId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        assertThat(result.getNumber()).isEqualTo("4444333322221111");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository).existsByNumber(dto.getNumber());
        verify(userRepository).findById(dto.getUserId());
        verify(cardInfoMapper).updateEntity(existingCard, dto);
        verify(cardInfoRepository).save(existingCard);
        verify(cardInfoMapper).toDto(updatedCard);
    }

    @Test
    void update_ShouldUpdateCardNumber_WhenUserRemainsTheSame() {

        Long cardId = 1L;
        Long sameUserId = 10L;

        CardInfo existingCard = new CardInfo();
        existingCard.setId(cardId);
        existingCard.setNumber("1111222233334444");

        User sameUser = new User();
        sameUser.setId(sameUserId);
        existingCard.setUser(sameUser);

        CardInfoRequest dto = new CardInfoRequest();
        dto.setNumber("4444333322221111");
        dto.setUserId(sameUserId);

        CardInfo updatedCard = new CardInfo();
        updatedCard.setId(cardId);
        updatedCard.setNumber("4444333322221111");
        updatedCard.setUser(sameUser);

        CardInfoResponse response = new CardInfoResponse();
        response.setId(cardId);
        response.setNumber("4444333322221111");

        when(cardInfoRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(cardInfoRepository.existsByNumber(dto.getNumber())).thenReturn(false);
        when(cardInfoRepository.save(existingCard)).thenReturn(updatedCard);
        when(cardInfoMapper.toDto(updatedCard)).thenReturn(response);

        CardInfoResponse result = cardInfoService.update(cardId, dto);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(cardId);
        assertThat(result.getNumber()).isEqualTo("4444333322221111");

        verify(cardInfoRepository).findById(cardId);
        verify(cardInfoRepository).existsByNumber(dto.getNumber());
        verify(cardInfoMapper).updateEntity(existingCard, dto);
        verify(cardInfoRepository).save(existingCard);
        verify(cardInfoMapper).toDto(updatedCard);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void delete_ShouldDeleteCard_WhenExists() {

        Long cardId = 1L;
        when(cardInfoRepository.existsById(cardId)).thenReturn(true);

        cardInfoService.delete(cardId);

        verify(cardInfoRepository).existsById(cardId);
        verify(cardInfoRepository).deleteById(cardId);
    }

    @Test
    void delete_ShouldThrowNotFoundException_WhenCardNotFound() {

        Long cardId = 1488L;
        when(cardInfoRepository.existsById(cardId)).thenReturn(false);

        assertThatThrownBy(() -> cardInfoService.delete(cardId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card id=1488 not found");

        verify(cardInfoRepository).existsById(cardId);
    }
}

