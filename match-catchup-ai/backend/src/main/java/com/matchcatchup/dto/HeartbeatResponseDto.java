package com.matchcatchup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatResponseDto {

    private String userId;
    private String matchId;
    private String activityStatus;
    private LocalDateTime lastHeartbeatAt;
    private Long lastViewedDeliveryId;
    private Long latestCollectedDeliveryId;
    private boolean catchupAvailable;
    private CatchupSessionDto catchupSession;
}
