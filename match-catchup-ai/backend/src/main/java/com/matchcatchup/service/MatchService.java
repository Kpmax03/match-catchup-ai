package com.matchcatchup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.matchcatchup.dto.LiveScoreCacheDto;
import com.matchcatchup.dto.LiveScoreDto;
import com.matchcatchup.dto.MatchResponseDto;
import com.matchcatchup.dto.TimelineDeliveryDto;
import com.matchcatchup.entity.CollectedDelivery;
import com.matchcatchup.entity.MatchCollectionState;
import com.matchcatchup.entity.MatchEntity;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import com.matchcatchup.repository.MatchCollectionStateRepository;
import com.matchcatchup.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final MatchRepository matchRepository;
    private final MatchCollectionStateRepository collectionStateRepository;
    private final CollectedDeliveryRepository deliveryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String REDIS_KEY_PREFIX = "match:";
    private static final String REDIS_LIVE_SUFFIX = ":live";

    public List<MatchResponseDto> getAllMatches() {
        List<MatchEntity> matches = matchRepository.findAll();
        if (matches.isEmpty()) {
            // Default seed for IND-AUS-001 if no matches saved yet
            return Collections.singletonList(
                    MatchResponseDto.builder()
                            .id("IND-AUS-001")
                            .matchName("India vs Australia")
                            .format("T20")
                            .status("LIVE")
                            .battingTeam("India")
                            .bowlingTeam("Australia")
                            .totalScore(0)
                            .wickets(0)
                            .overs(0)
                            .balls(0)
                            .runRate(0.0)
                            .lastCollectedDeliveryId(0L)
                            .build()
            );
        }

        return matches.stream().map(this::mapToMatchResponse).collect(Collectors.toList());
    }

    public MatchResponseDto getMatchById(String matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found: " + matchId));
        return mapToMatchResponse(match);
    }

    /**
     * Reads the latest live score.
     * 1. Checks Redis cache first.
     * 2. Falls back to MySQL if Redis entry expired or unavailable.
     * Guaranteed NEVER to call Match Score Generator endpoint!
     */
    public LiveScoreDto getLiveScore(String matchId) {
        String redisKey = REDIS_KEY_PREFIX + matchId + REDIS_LIVE_SUFFIX;

        // Step 1: Attempt reading from Redis
        try {
            Object cachedObj = redisTemplate.opsForValue().get(redisKey);
            LiveScoreCacheDto cacheDto = null;
            if (cachedObj instanceof LiveScoreCacheDto) {
                cacheDto = (LiveScoreCacheDto) cachedObj;
            } else if (cachedObj instanceof Map) {
                cacheDto = objectMapper.convertValue(cachedObj, LiveScoreCacheDto.class);
            }

            if (cacheDto != null) {
                log.debug("Live score cache HIT for {}", redisKey);
                return LiveScoreDto.builder()
                        .matchId(cacheDto.getMatchId())
                        .format(cacheDto.getFormat())
                        .status(cacheDto.getStatus())
                        .innings(cacheDto.getInnings())
                        .battingTeam(cacheDto.getBattingTeam())
                        .bowlingTeam(cacheDto.getBowlingTeam())
                        .totalScore(cacheDto.getTotalScore())
                        .wickets(cacheDto.getWickets())
                        .overs(cacheDto.getOvers())
                        .balls(cacheDto.getBalls())
                        .runRate(cacheDto.getRunRate())
                        .target(cacheDto.getTarget())
                        .latestDeliveryId(cacheDto.getLatestDeliveryId())
                        .striker(cacheDto.getStriker())
                        .strikerRuns(cacheDto.getStrikerRuns())
                        .strikerBalls(cacheDto.getStrikerBalls())
                        .nonStriker(cacheDto.getNonStriker())
                        .nonStrikerRuns(cacheDto.getNonStrikerRuns())
                        .nonStrikerBalls(cacheDto.getNonStrikerBalls())
                        .bowler(cacheDto.getBowler())
                        .bowlerWickets(cacheDto.getBowlerWickets())
                        .bowlerRuns(cacheDto.getBowlerRuns())
                        .bowlerOvers(cacheDto.getBowlerOvers())
                        .source("REDIS")
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Error reading from Redis key {}: {}", redisKey, ex.getMessage());
        }

        // Step 2: Fallback to MySQL
        log.info("Redis cache MISS for {}. Falling back to MySQL.", redisKey);
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found in database: " + matchId));

        Long lastDeliveryId = collectionStateRepository.findByMatchId(matchId)
                .map(MatchCollectionState::getLastCollectedDeliveryId)
                .orElse(0L);

        List<CollectedDelivery> allDeliveries = deliveryRepository.findByMatchIdOrderByProviderDeliveryIdAsc(matchId);
        BroadcastStatsHelper.BroadcastFigures figures = BroadcastStatsHelper.calculateFigures(allDeliveries, match.getInnings());

        return LiveScoreDto.builder()
                .matchId(match.getId())
                .format(match.getFormat())
                .status(match.getStatus())
                .innings(match.getInnings())
                .battingTeam(match.getBattingTeam())
                .bowlingTeam(match.getBowlingTeam())
                .totalScore(match.getTotalScore())
                .wickets(match.getWickets())
                .overs(match.getOvers())
                .balls(match.getBalls())
                .runRate(match.getRunRate())
                .target(match.getTarget())
                .latestDeliveryId(lastDeliveryId)
                .striker(figures.getStriker())
                .strikerRuns(figures.getStrikerRuns())
                .strikerBalls(figures.getStrikerBalls())
                .nonStriker(figures.getNonStriker())
                .nonStrikerRuns(figures.getNonStrikerRuns())
                .nonStrikerBalls(figures.getNonStrikerBalls())
                .bowler(figures.getBowler())
                .bowlerWickets(figures.getBowlerWickets())
                .bowlerRuns(figures.getBowlerRuns())
                .bowlerOvers(figures.getBowlerOvers())
                .source("DATABASE")
                .build();
    }

    private MatchResponseDto mapToMatchResponse(MatchEntity match) {
        Long lastDeliveryId = collectionStateRepository.findByMatchId(match.getId())
                .map(MatchCollectionState::getLastCollectedDeliveryId)
                .orElse(0L);

        return MatchResponseDto.builder()
                .id(match.getId())
                .matchName(match.getMatchName())
                .format(match.getFormat())
                .status(match.getStatus())
                .innings(match.getInnings())
                .battingTeam(match.getBattingTeam())
                .bowlingTeam(match.getBowlingTeam())
                .totalScore(match.getTotalScore())
                .wickets(match.getWickets())
                .overs(match.getOvers())
                .balls(match.getBalls())
                .runRate(match.getRunRate())
                .target(match.getTarget())
                .lastCollectedDeliveryId(lastDeliveryId)
                .updatedAt(match.getUpdatedAt())
                .build();
    }

    public List<TimelineDeliveryDto> getRecentDeliveries(String matchId) {
        return deliveryRepository.findTop30ByMatchIdOrderByProviderDeliveryIdDesc(matchId).stream()
                .map(d -> TimelineDeliveryDto.builder()
                        .id(d.getId())
                        .providerDeliveryId(d.getProviderDeliveryId())
                        .innings(d.getInnings())
                        .overNumber(d.getOverNumber())
                        .ballNumber(d.getBallNumber())
                        .batter(d.getBatter())
                        .bowler(d.getBowler())
                        .runs(d.getRuns())
                        .extras(d.getExtras())
                        .totalRuns(d.getTotalRuns())
                        .wicket(d.isWicket())
                        .legalDelivery(d.isLegalDelivery())
                        .wicketType(d.getWicketType())
                        .eventType(d.getEventType())
                        .eventDescription(d.getEventDescription())
                        .collectedAt(d.getCollectedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
