package com.matchcatchup.controller;

import com.matchcatchup.dto.LiveScoreDto;
import com.matchcatchup.dto.MatchResponseDto;
import com.matchcatchup.service.CollectionService;
import com.matchcatchup.service.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MatchController {

    private final MatchService matchService;
    private final CollectionService collectionService;

    @GetMapping
    public List<MatchResponseDto> getAllMatches() {
        return matchService.getAllMatches();
    }

    @GetMapping("/{matchId}")
    public MatchResponseDto getMatchById(@PathVariable String matchId) {
        return matchService.getMatchById(matchId);
    }

    /**
     * Reads the latest live score from Redis (or MySQL fallback).
     * Guaranteed never to advance the simulation.
     */
    @GetMapping("/{matchId}/live")
    public LiveScoreDto getLiveScore(@PathVariable String matchId) {
        return matchService.getLiveScore(matchId);
    }

    @GetMapping("/{matchId}/feed")
    public List<com.matchcatchup.dto.TimelineDeliveryDto> getLiveFeed(@PathVariable String matchId) {
        return matchService.getRecentDeliveries(matchId);
    }

    /**
     * Explicitly resets the match simulation to restart from delivery ID 1.
     */
    @PostMapping("/{matchId}/reset")
    public Map<String, String> resetMatchSimulation(@PathVariable String matchId) {
        log.info("API trigger: Resetting match simulation for {} to restart from delivery ID 1", matchId);
        collectionService.resetMatchToStart(matchId);
        try {
            collectionService.collectNextDelivery(matchId);
        } catch (Exception ex) {
            log.warn("Immediate collection after API reset deferred: {}", ex.getMessage());
        }
        return Collections.singletonMap("message", "Match " + matchId + " successfully reset to delivery ID 1.");
    }
}
