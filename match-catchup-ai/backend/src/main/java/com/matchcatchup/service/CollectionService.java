package com.matchcatchup.service;

import com.matchcatchup.client.MatchScoreGeneratorClient;
import com.matchcatchup.client.dto.ProviderDeliveryDto;
import com.matchcatchup.client.dto.ProviderLiveScoreDto;
import com.matchcatchup.dto.LiveScoreCacheDto;
import com.matchcatchup.entity.CollectedDelivery;
import com.matchcatchup.entity.MatchCollectionState;
import com.matchcatchup.entity.MatchEntity;
import com.matchcatchup.repository.CatchupSessionRepository;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import com.matchcatchup.repository.MatchCollectionStateRepository;
import com.matchcatchup.repository.MatchRepository;
import com.matchcatchup.repository.UserMatchActivityRepository;
import feign.FeignException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionService {

    private final MatchScoreGeneratorClient scoreGeneratorClient;
    private final MatchRepository matchRepository;
    private final CollectedDeliveryRepository deliveryRepository;
    private final MatchCollectionStateRepository collectionStateRepository;
    private final CatchupSessionRepository catchupSessionRepository;
    private final UserMatchActivityRepository activityRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${matchcatchup.redis.live-score-ttl-seconds:21600}")
    private long redisTtlSeconds;

    private static final String REDIS_KEY_PREFIX = "match:";
    private static final String REDIS_LIVE_SUFFIX = ":live";

    /**
     * Resets match collection and associated session data to restart cleanly from delivery ID 1.
     */
    @Transactional
    public void resetMatchToStart(String matchId) {
        log.info("Resetting match {} collection state to restart from delivery ID 1...", matchId);

        // 1. Reset collection cursor to 0 so next call to getNextLiveScore(0) retrieves delivery 1
        MatchCollectionState state = collectionStateRepository.findByMatchId(matchId)
                .orElseGet(() -> MatchCollectionState.builder()
                        .matchId(matchId)
                        .lastCollectedDeliveryId(0L)
                        .collectionStatus("COLLECTING")
                        .lastCollectionTime(LocalDateTime.now())
                        .build());

        state.setLastCollectedDeliveryId(0L);
        state.setCollectionStatus("COLLECTING");
        state.setLastCollectionTime(LocalDateTime.now());
        collectionStateRepository.save(state);

        // 2. Clear previously collected deliveries for this match
        deliveryRepository.deleteByMatchId(matchId);

        // 3. Clear previous catch-up sessions for this match
        catchupSessionRepository.deleteByMatchId(matchId);

        // 4. Reset user match viewing activities for this match
        activityRepository.deleteByMatchId(matchId);

        // 5. Reset MatchEntity in MySQL
        matchRepository.findById(matchId).ifPresent(match -> {
            match.setTotalScore(0);
            match.setWickets(0);
            match.setOvers(0);
            match.setBalls(0);
            match.setRunRate(0.0);
            match.setStatus("LIVE");
            match.setInnings(1);
            match.setBattingTeam("India");
            match.setBowlingTeam("Australia");
            match.setTarget(null);
            match.setUpdatedAt(LocalDateTime.now());
            matchRepository.save(match);
        });

        // 6. Evict Redis live score cache for this match
        try {
            String redisKey = REDIS_KEY_PREFIX + matchId + REDIS_LIVE_SUFFIX;
            redisTemplate.delete(redisKey);
            log.info("Evicted Redis live score cache for {}", redisKey);
        } catch (Exception ex) {
            log.warn("Failed to evict Redis cache on reset: {}", ex.getMessage());
        }

        log.info("Match {} successfully reset to delivery cursor 0.", matchId);
    }

    /**
     * Executes one collection step for the match.
     * Guaranteed to be restart-safe, transactional, and idempotency-enforced.
     */
    @Transactional
    public boolean collectNextDelivery(String matchId) {
        // 1. Fetch or initialize collection cursor
        MatchCollectionState state = collectionStateRepository.findByMatchId(matchId)
                .orElseGet(() -> {
                    MatchCollectionState newState = MatchCollectionState.builder()
                            .matchId(matchId)
                            .lastCollectedDeliveryId(0L)
                            .collectionStatus("COLLECTING")
                            .lastCollectionTime(LocalDateTime.now())
                            .build();
                    return collectionStateRepository.save(newState);
                });

        if ("COMPLETED".equalsIgnoreCase(state.getCollectionStatus())) {
            log.info("Collection for match {} already marked COMPLETED", matchId);
            return false;
        }

        Long currentCursor = state.getLastCollectedDeliveryId();
        log.info("Requesting next delivery for match {} at cursor {}", matchId, currentCursor);

        ProviderLiveScoreDto responseDto;
        try {
            responseDto = scoreGeneratorClient.getNextLiveScore(currentCursor);
        } catch (FeignException.NotFound ex) {
            log.info("Provider returned 404 for cursor {} — match simulation complete or no more deliveries", currentCursor);
            state.setCollectionStatus("COMPLETED");
            state.setLastCollectionTime(LocalDateTime.now());
            collectionStateRepository.save(state);
            return false;
        } catch (Exception ex) {
            log.error("Failed to collect delivery from provider for cursor {}: {}", currentCursor, ex.getMessage());
            state.setCollectionStatus("RETRYING");
            state.setLastCollectionTime(LocalDateTime.now());
            collectionStateRepository.save(state);
            return false;
        }

        if (responseDto == null || responseDto.getLatestDelivery() == null) {
            log.warn("Provider returned empty response for cursor {}", currentCursor);
            return false;
        }

        ProviderDeliveryDto deliveryDto = responseDto.getLatestDelivery();
        Long providerDeliveryId = deliveryDto.getId();

        // 2. Persist Delivery if not already saved (Enforcing uniqueness matchId + providerDeliveryId)
        if (!deliveryRepository.existsByMatchIdAndProviderDeliveryId(matchId, providerDeliveryId)) {
            CollectedDelivery collectedDelivery = CollectedDelivery.builder()
                    .matchId(matchId)
                    .providerDeliveryId(providerDeliveryId)
                    .innings(deliveryDto.getInnings())
                    .overNumber(deliveryDto.getOver())
                    .ballNumber(deliveryDto.getBall())
                    .batter(deliveryDto.getBatter())
                    .bowler(deliveryDto.getBowler())
                    .runs(deliveryDto.getRuns())
                    .extras(deliveryDto.getExtras())
                    .totalRuns(deliveryDto.getTotalRuns())
                    .wicket(deliveryDto.isWicket())
                    .legalDelivery(deliveryDto.isLegalDelivery())
                    .wicketType(deliveryDto.getWicketType())
                    .eventType(deliveryDto.getEventType())
                    .eventDescription(deliveryDto.getEventDescription())
                    .collectedAt(LocalDateTime.now())
                    .build();

            deliveryRepository.save(collectedDelivery);
            log.info("Saved delivery id={} (over {}.{}) for match {}",
                    providerDeliveryId, deliveryDto.getOver(), deliveryDto.getBall(), matchId);
        }

        // 3. Update Match State entity in MySQL
        MatchEntity match = matchRepository.findById(matchId)
                .orElseGet(() -> MatchEntity.builder()
                        .id(matchId)
                        .externalMatchId(matchId)
                        .matchName("India vs Australia")
                        .format(responseDto.getFormat() != null ? responseDto.getFormat() : "T20")
                        .status(responseDto.getStatus() != null ? responseDto.getStatus() : "LIVE")
                        .createdAt(LocalDateTime.now())
                        .build());

        match.setFormat(responseDto.getFormat());
        match.setStatus(responseDto.getStatus());
        match.setInnings(responseDto.getInnings());
        match.setBattingTeam(responseDto.getBattingTeam());
        match.setBowlingTeam(responseDto.getBowlingTeam());
        match.setTotalScore(responseDto.getTotalScore());
        match.setWickets(responseDto.getWickets());
        match.setOvers(responseDto.getOvers());
        match.setBalls(responseDto.getBalls());
        match.setRunRate(responseDto.getRunRate());
        match.setTarget(responseDto.getTarget());
        match.setUpdatedAt(LocalDateTime.now());
        matchRepository.save(match);

        // 4. Advance collection cursor in MySQL
        state.setLastCollectedDeliveryId(providerDeliveryId);
        state.setLastCollectionTime(LocalDateTime.now());
        state.setCollectionStatus("COMPLETED".equalsIgnoreCase(responseDto.getStatus()) ? "COMPLETED" : "COLLECTING");
        collectionStateRepository.save(state);

        // 5. Cache latest live score in Redis with 6-hour TTL
        cacheLiveScoreInRedis(matchId, responseDto, providerDeliveryId);

        log.info("Collection succeeded for match {}: Cursor advanced to {}", matchId, providerDeliveryId);
        return true;
    }

    private void cacheLiveScoreInRedis(String matchId, ProviderLiveScoreDto scoreDto, Long latestDeliveryId) {
        try {
            String redisKey = REDIS_KEY_PREFIX + matchId + REDIS_LIVE_SUFFIX;

            // Compute authoritative match figures for broadcast scorecard
            List<CollectedDelivery> allDeliveries = deliveryRepository.findByMatchIdOrderByProviderDeliveryIdAsc(matchId);
            BroadcastStatsHelper.BroadcastFigures figures = BroadcastStatsHelper.calculateFigures(allDeliveries, scoreDto.getInnings());

            LiveScoreCacheDto cacheDto = LiveScoreCacheDto.builder()
                    .matchId(matchId)
                    .format(scoreDto.getFormat())
                    .status(scoreDto.getStatus())
                    .innings(scoreDto.getInnings())
                    .battingTeam(scoreDto.getBattingTeam())
                    .bowlingTeam(scoreDto.getBowlingTeam())
                    .totalScore(scoreDto.getTotalScore())
                    .wickets(scoreDto.getWickets())
                    .overs(scoreDto.getOvers())
                    .balls(scoreDto.getBalls())
                    .runRate(scoreDto.getRunRate())
                    .target(scoreDto.getTarget())
                    .latestDeliveryId(latestDeliveryId)
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
                    .build();

            redisTemplate.opsForValue().set(redisKey, cacheDto, Duration.ofSeconds(redisTtlSeconds));
            log.info("Cached latest score in Redis key {} with TTL {}s", redisKey, redisTtlSeconds);
        } catch (Exception ex) {
            log.warn("Redis caching unavailable or failed: {}", ex.getMessage());
        }
    }
}
