package com.matchcatchup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineDeliveryDto {

    private Long id;
    private Long providerDeliveryId;
    private int innings;
    private int overNumber;
    private int ballNumber;
    private String batter;
    private String bowler;
    private int runs;
    private int extras;
    private int totalRuns;
    private int inningsScoreAfterDelivery;
    private int inningsWicketsAfterDelivery;
    private boolean wicket;
    private boolean legalDelivery;
    private String wicketType;
    private String eventType;
    private String eventDescription;
    private LocalDateTime collectedAt;
}
