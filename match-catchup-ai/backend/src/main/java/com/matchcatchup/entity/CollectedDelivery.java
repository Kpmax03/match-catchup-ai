package com.matchcatchup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "collected_deliveries", uniqueConstraints = @UniqueConstraint(name = "uk_collected_match_provider_delivery", columnNames = {"match_id", "provider_delivery_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectedDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_delivery_id", nullable = false)
    private Long providerDeliveryId;

    @Column(name = "match_id", length = 50, nullable = false)
    private String matchId;

    @Column(name = "innings", nullable = false)
    private int innings;

    @Column(name = "over_number", nullable = false)
    private int overNumber;

    @Column(name = "ball_number", nullable = false)
    private int ballNumber;

    @Column(name = "batter", length = 100)
    private String batter;

    @Column(name = "bowler", length = 100)
    private String bowler;

    @Column(name = "runs", nullable = false)
    private int runs;

    @Column(name = "extras", nullable = false)
    private int extras;

    @Column(name = "total_runs", nullable = false)
    private int totalRuns;

    @Column(name = "wicket", nullable = false)
    private boolean wicket;

    @Column(name = "legal_delivery", nullable = false)
    private boolean legalDelivery;

    @Column(name = "wicket_type", length = 50)
    private String wicketType;

    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "event_description", length = 255)
    private String eventDescription;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    @PrePersist
    public void prePersist() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}
