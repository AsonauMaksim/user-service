package com.internship.userservice.repository;

import com.internship.userservice.entity.CardInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CardInfoRepository extends JpaRepository<CardInfo, Long> {

    List<CardInfo> findAllByIdIn(List<Long> ids);

    CardInfo findByNumber(String number);

    @Query("SELECT c FROM CardInfo c WHERE c.user.id = :userId")
    List<CardInfo> findByUserId(Long userId);

    @Query(value = "SELECT * FROM card_info WHERE number LIKE :prefix%", nativeQuery = true)
    List<CardInfo> findByNumberPrefix(String prefix);
}