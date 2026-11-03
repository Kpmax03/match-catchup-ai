package com.matchcatchup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatusResponseDto {

    private String userId;
    private String matchId;
    private String activityStatus;
    private LocalDateTime lastHeartbeatAt;
    private LocalDateTime inactiveFrom;
    private Long lastViewedDeliveryId;
    private Long latestCollectedDeliveryId;
    private boolean catchupAvailable;
    private CatchupSessionDto activeCatchupSession;
}
