package com.match_score_generator.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreResponseDto {

    private String matchId;
    private String format;
    private String status;

    private int innings;
    private String battingTeam;
    private String bowlingTeam;

    private int totalScore;
    private int wickets;

    private int overs;
    private int balls;

    private double runRate;
    private Integer target;

    private DeliveryResponseDto latestDelivery;
}
