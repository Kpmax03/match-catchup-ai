package com.matchcatchup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_match_activities", uniqueConstraints = @UniqueConstraint(name = "uk_user_match_activity", columnNames = {"user_id", "match_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMatchActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "match_id", length = 50, nullable = false)
    private String matchId;

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "inactive_from")
    private LocalDateTime inactiveFrom;

    @Column(name = "activity_status", length = 20, nullable = false)
    private String activityStatus; // "ACTIVE" or "INACTIVE"

    @Column(name = "last_viewed_delivery_id", nullable = false)
    private Long lastViewedDeliveryId;
}
