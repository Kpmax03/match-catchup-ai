package com.match_score_generator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int innings;

    private int overNumber;

    private int ballNumber;

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
