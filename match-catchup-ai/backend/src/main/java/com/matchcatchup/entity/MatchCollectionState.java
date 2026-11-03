package com.matchcatchup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_collection_states", uniqueConstraints = @UniqueConstraint(name = "uk_match_collection_state_match", columnNames = "match_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchCollectionState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_id", length = 50, nullable = false)
    private String matchId;

    @Column(name = "last_collected_delivery_id", nullable = false)
    private Long lastCollectedDeliveryId;

    @Column(name = "last_collection_time")
    private LocalDateTime lastCollectionTime;

    @Column(name = "collection_status", length = 30, nullable = false)
    private String collectionStatus;
}
