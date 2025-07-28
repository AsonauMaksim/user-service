package com.internship.userservice.dto.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
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
