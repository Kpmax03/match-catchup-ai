package com.matchcatchup.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResponseDto {

    private String id;
    private String matchName;
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
    private Long lastCollectedDeliveryId;
    private LocalDateTime updatedAt;
}
