package com.internship.userservice.mapper;

import com.internship.userservice.dto.card.CardInfoRequest;
import com.internship.userservice.dto.card.CardInfoResponse;
import com.internship.userservice.entity.CardInfo;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardInfoMapper {

    CardInfo toEntity(CardInfoRequest dto);

    @Mapping(target = "userId", source = "user.id")
    CardInfoResponse toDto(CardInfo entity);

    List<CardInfoResponse> toDtoList(List<CardInfo> cards);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget CardInfo entity, CardInfoRequest dto);
}
