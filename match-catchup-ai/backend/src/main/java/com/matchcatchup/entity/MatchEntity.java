package com.matchcatchup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "external_match_id", length = 50)
    private String externalMatchId;

    @Column(name = "match_name", length = 100, nullable = false)
    private String matchName;

    @Column(name = "format", length = 20, nullable = false)
    private String format;

    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "innings")
    private int innings;

    @Column(name = "batting_team", length = 50)
    private String battingTeam;

    @Column(name = "bowling_team", length = 50)
    private String bowlingTeam;

    @Column(name = "total_score")
    private int totalScore;

    @Column(name = "wickets")
    private int wickets;

    @Column(name = "overs")
    private int overs;

    @Column(name = "balls")
    private int balls;

    @Column(name = "run_rate")
    private double runRate;

    @Column(name = "target")
    private Integer target;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
