package com.matchcatchup.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineResponseDto {

    private String sessionId;
    private String matchId;
    private Long fromDeliveryId;
    private Long toDeliveryId;
    private int totalDeliveries;
    private int totalRuns;
    private int totalWickets;
    private int totalFours;
    private int totalSixes;
    private List<TimelineOverGroupDto> overs;
}
