package com.internship.userservice.dto.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardInfoResponse {

    private Long id;
    private Long userId;
    private String number;
    private String holder;
    private String expirationDate;
}
