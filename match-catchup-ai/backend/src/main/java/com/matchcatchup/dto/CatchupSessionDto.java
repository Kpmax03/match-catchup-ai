package com.matchcatchup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatchupSessionDto {

    private String id;
    private String userId;
    private String matchId;
    private LocalDateTime inactiveFrom;
    private LocalDateTime returnedAt;
    private Long fromDeliveryId;
    private Long toDeliveryId;
    private int missedDeliveriesCount;
    private String fromOver;
    private String toOver;
    private String status;
    private String summary;
    private LocalDateTime createdAt;
}
