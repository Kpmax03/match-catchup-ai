package com.matchcatchup.client.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderDeliveryDto {

    private Long id;
    private int innings;
    private int over;
    private int ball;
    private String batter;
    private String bowler;
    private int runs;
    private int extras;
    private int totalRuns;
    private boolean wicket;
    private boolean legalDelivery;
    private String wicketType;
    private String eventType;
    private String eventDescription;
}
