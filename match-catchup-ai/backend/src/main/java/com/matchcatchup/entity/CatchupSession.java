package com.matchcatchup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "catchup_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatchupSession {

    @Id
    @Column(name = "id", length = 64)
    private String id; // UUID string

    @Column(name = "user_id", length = 50, nullable = false)
    private String userId;

    @Column(name = "match_id", length = 50, nullable = false)
    private String matchId;

    @Column(name = "inactive_from", nullable = false)
    private LocalDateTime inactiveFrom;

    @Column(name = "returned_at", nullable = false)
    private LocalDateTime returnedAt;

    @Column(name = "from_delivery_id", nullable = false)
    private Long fromDeliveryId;

    @Column(name = "to_delivery_id", nullable = false)
    private Long toDeliveryId;

    @Column(name = "status", length = 30, nullable = false)
    private String status; // "PENDING", "ACTIVE", "COMPLETED", "DISMISSED"

    @Lob
    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "vector_indexed", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    @Builder.Default
    private boolean vectorIndexed = false;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
