package com.matchcatchup.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewedDeliveryRequestDto {

    private String userId;
    private Long deliveryId;
}
