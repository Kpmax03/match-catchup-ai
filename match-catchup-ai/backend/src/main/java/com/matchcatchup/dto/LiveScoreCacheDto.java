package com.matchcatchup.dto;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveScoreCacheDto implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private Long latestDeliveryId;

    // Synchronized real-time batter and bowler broadcast figures
    private String striker;
    private int strikerRuns;
    private int strikerBalls;
    private String nonStriker;
    private int nonStrikerRuns;
    private int nonStrikerBalls;
    private String bowler;
    private int bowlerWickets;
    private int bowlerRuns;
    private double bowlerOvers;
}
