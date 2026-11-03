package com.match_score_generator.service;

import com.match_score_generator.entity.LiveScore;
import com.match_score_generator.repository.LiveScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class MatchDataInitializer implements CommandLineRunner {

    private final LiveScoreRepository liveScoreRepository;

    @Override
    public void run(String... args) {

        LiveScore liveScore = LiveScore.builder()
                .matchId("IND-AUS-001")
                .format("T20")
                .status("LIVE")
                .innings(1)
                .battingTeam("India")
                .bowlingTeam("Australia")
                .totalScore(0)
                .wickets(0)
                .overs(0)
                .balls(0)
                .runRate(0.0)
                .target(null)
                .lastUpdated(LocalDateTime.now())
                .build();

        liveScoreRepository.save(liveScore);
    }
}