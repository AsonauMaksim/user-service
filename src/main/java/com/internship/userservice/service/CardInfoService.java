package com.internship.userservice.service;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;

import java.util.List;

public interface CardInfoService {

    CardInfoResponse create(CardInfoRequest dto, Long userCredentialsId);

    CardInfoResponse getCardById(Long id);

    List<CardInfoResponse> getAllByIds(List<Long> ids);

    List<CardInfoResponse> getByUserId(Long userId);

    CardInfoResponse update(Long id, CardInfoRequest dto, Long userCredentialsId);

    void delete(Long id, Long userCredentialsId);
}
