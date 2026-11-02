package com.match_score_generator.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveScore {

    @Id
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

    private LocalDateTime lastUpdated;
}
