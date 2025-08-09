package com.internship.userservice.service.impl;


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
import com.internship.userservice.service.CardInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardInfoServiceImpl implements CardInfoService {

    private final CardInfoRepository cardInfoRepository;
    private final UserRepository userRepository;
    private final CardInfoMapper cardInfoMapper;

    @Override
    @Transactional
    public CardInfoResponse create(CardInfoRequest dto) {

        Long userCredentialsId = JwtUtils.getUserCredentialsIdFromToken();

        User owner = userRepository.findByUserCredentialsId(userCredentialsId)
                .orElseThrow(() -> new NotFoundException("User with credentials id=" + userCredentialsId + " not found"));

        if (cardInfoRepository.existsByNumber(dto.getNumber())) {
            throw new AlreadyExistsException("Card number '" + dto.getNumber() + "' already exists");
        }

        CardInfo card = cardInfoMapper.toEntity(dto);
        card.setUser(owner);

        card = cardInfoRepository.save(card);
        return cardInfoMapper.toDto(card);
    }

    @Override
    @Cacheable(value = "cards", key = "#id")
    public CardInfoResponse getCardById(Long id) {
        return cardInfoMapper.toDto(cardInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card id=" + id + " not found")));
    }

    @Override
    public List<CardInfoResponse> getAllByIds(List<Long> ids) {
        return cardInfoMapper.toDtoList(cardInfoRepository.findAllById(ids));
    }

    @Override
    public List<CardInfoResponse> getByUserId(Long userId) {
        return cardInfoMapper.toDtoList(cardInfoRepository.findByUserId(userId));
    }

    @Override
    @Transactional
    @CachePut(value = "cards", key = "#id")
    public CardInfoResponse update(Long id, CardInfoRequest dto) {

        Long userCredentialsId = JwtUtils.getUserCredentialsIdFromToken();

        CardInfo card = cardInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card id=" + id + " not found"));

        if (!card.getUser().getUserCredentialsId().equals(userCredentialsId)) {
            throw new AccessDeniedException("Access denied: you can only update your own cards");
        }

        if (!card.getNumber().equals(dto.getNumber()) &&
                cardInfoRepository.existsByNumber(dto.getNumber())) {
            throw new AlreadyExistsException("Card number '" + dto.getNumber() + "' already exists");
        }

        cardInfoMapper.updateEntity(card, dto);
        card = cardInfoRepository.save(card);

        return cardInfoMapper.toDto(card);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public void delete(Long id) {

        Long userCredentialsId = JwtUtils.getUserCredentialsIdFromToken();

        CardInfo card = cardInfoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card id=" + id + " not found"));

        if (!card.getUser().getUserCredentialsId().equals(userCredentialsId)) {
            throw new AccessDeniedException("Access denied: you can only delete your own cards");
        }

        cardInfoRepository.deleteById(id);
    }
}