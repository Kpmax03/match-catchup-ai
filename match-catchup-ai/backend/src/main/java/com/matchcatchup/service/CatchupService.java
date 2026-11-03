package com.matchcatchup.service;

import com.matchcatchup.dto.*;
import com.matchcatchup.entity.CatchupSession;
import com.matchcatchup.entity.CollectedDelivery;
import com.matchcatchup.entity.MatchCollectionState;
import com.matchcatchup.entity.UserMatchActivity;
import com.matchcatchup.repository.CatchupSessionRepository;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import com.matchcatchup.repository.MatchCollectionStateRepository;
import com.matchcatchup.repository.UserMatchActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatchupService {

    private final CatchupSessionRepository sessionRepository;
    private final CollectedDeliveryRepository deliveryRepository;
    private final UserMatchActivityRepository activityRepository;
    private final MatchCollectionStateRepository collectionStateRepository;
    private final SummaryService summaryService;
    private final ActivityService activityService;

    public CatchupSessionDto getSessionById(String sessionId, String userId) {
        CatchupSession session = ownedSession(sessionId, userId);

        // Ensure summary is populated
        if (session.getSummary() == null || session.getSummary().trim().isEmpty()) {
            summaryService.getOrGenerateSummary(session);
        }

        return activityService.mapToSessionDto(session);
    }

    public CatchupSessionDto getAvailableCatchupSession(String matchId, String userId) {
        return sessionRepository.findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(userId, matchId, "PENDING")
                .map(session -> {
                    if (session.getSummary() == null || session.getSummary().trim().isEmpty()) {
                        summaryService.getOrGenerateSummary(session);
                    }
                    return activityService.mapToSessionDto(session);
                })
                .orElse(null);
    }

    public String getSessionSummary(String sessionId, String userId) {
        CatchupSession session = ownedSession(sessionId, userId);
        return summaryService.getOrGenerateSummary(session);
    }

    /**
     * Retrieves all deliveries strictly belonging to the catch-up session interval [fromDeliveryId, toDeliveryId].
     * Grouped by over in chronological order.
     */
    public TimelineResponseDto getTimelineForSession(String sessionId, String userId) {
        CatchupSession session = ownedSession(sessionId, userId);

        List<CollectedDelivery> deliveries = deliveryRepository
                .findByMatchIdAndProviderDeliveryIdBetweenOrderByProviderDeliveryIdAsc(
                        session.getMatchId(), session.getFromDeliveryId(), session.getToDeliveryId()
                );

        int totalDeliveries = deliveries.size();
        int totalRuns = deliveries.stream().mapToInt(CollectedDelivery::getTotalRuns).sum();
        int totalWickets = (int) deliveries.stream().filter(CollectedDelivery::isWicket).count();
        int totalFours = (int) deliveries.stream().filter(d -> d.getRuns() == 4).count();
        int totalSixes = (int) deliveries.stream().filter(d -> d.getRuns() == 6).count();

        Map<Long, int[]> progression = new HashMap<>();
        Map<Integer, int[]> scoreByInnings = new HashMap<>();
        for (CollectedDelivery delivery : deliveryRepository.findByMatchIdOrderByProviderDeliveryIdAsc(session.getMatchId())) {
            int[] score = scoreByInnings.computeIfAbsent(delivery.getInnings(), ignored -> new int[2]);
            score[0] += delivery.getTotalRuns();
            if (delivery.isWicket()) score[1]++;
            if (delivery.getProviderDeliveryId() <= session.getToDeliveryId()) progression.put(delivery.getProviderDeliveryId(), new int[]{score[0], score[1]});
        }

        // Group by innings and over, retaining delivery chronology.
        Map<String, List<CollectedDelivery>> overMap = deliveries.stream()
                .collect(Collectors.groupingBy(d -> d.getInnings() + ":" + d.getOverNumber(), LinkedHashMap::new, Collectors.toList()));

        List<TimelineOverGroupDto> overGroups = new ArrayList<>();
        for (Map.Entry<String, List<CollectedDelivery>> entry : overMap.entrySet()) {
            List<CollectedDelivery> overDeliveries = entry.getValue();
            int overNumber = overDeliveries.get(0).getOverNumber();

            int overRuns = overDeliveries.stream().mapToInt(CollectedDelivery::getTotalRuns).sum();
            int overWickets = (int) overDeliveries.stream().filter(CollectedDelivery::isWicket).count();
            String bowler = overDeliveries.isEmpty() ? "Unknown" : overDeliveries.get(0).getBowler();
            int innings = overDeliveries.isEmpty() ? 1 : overDeliveries.get(0).getInnings();

            List<TimelineDeliveryDto> deliveryDtos = overDeliveries.stream()
                    .map(d -> mapToTimelineDelivery(d, progression.getOrDefault(d.getProviderDeliveryId(), new int[]{0, 0})))
                    .collect(Collectors.toList());

            overGroups.add(TimelineOverGroupDto.builder()
                    .innings(innings)
                    .overNumber(overNumber)
                    .totalRunsInOver(overRuns)
                    .wicketsInOver(overWickets)
                    .bowler(bowler)
                    .deliveries(deliveryDtos)
                    .build());
        }

        return TimelineResponseDto.builder()
                .sessionId(sessionId)
                .matchId(session.getMatchId())
                .fromDeliveryId(session.getFromDeliveryId())
                .toDeliveryId(session.getToDeliveryId())
                .totalDeliveries(totalDeliveries)
                .totalRuns(totalRuns)
                .totalWickets(totalWickets)
                .totalFours(totalFours)
                .totalSixes(totalSixes)
                .overs(overGroups)
                .build();
    }

    /**
     * Terminates the catch-up session and cleanly returns the user to the live match.
     * Synchronizes lastViewedDeliveryId with current live collected ball and marks user ACTIVE.
     */
    @Transactional
    public CatchupSessionDto completeCatchup(String sessionId, String userId) {
        CatchupSession session = ownedSession(sessionId, userId);

        session.setStatus("COMPLETED");
        sessionRepository.save(session);

        // Also terminate any other PENDING sessions for this user on this match
        List<CatchupSession> otherPending = sessionRepository.findByUserIdAndMatchIdAndStatus(userId, session.getMatchId(), "PENDING");
        for (CatchupSession s : otherPending) {
            s.setStatus("COMPLETED");
            sessionRepository.save(s);
        }

        // Current live collected delivery
        Long latestCollected = collectionStateRepository.findByMatchId(session.getMatchId())
                .map(MatchCollectionState::getLastCollectedDeliveryId)
                .orElse(session.getToDeliveryId());

        UserMatchActivity activity = activityRepository.findByUserIdAndMatchId(userId, session.getMatchId())
                .orElseGet(() -> UserMatchActivity.builder()
                        .userId(userId)
                        .matchId(session.getMatchId())
                        .build());

        // Bring user's viewing progress completely up-to-date with the live match
        Long syncDeliveryId = Math.max(session.getToDeliveryId(), latestCollected);
        activity.setLastViewedDeliveryId(syncDeliveryId);
        activity.setActivityStatus("ACTIVE");
        activity.setInactiveFrom(null);
        activity.setLastHeartbeatAt(LocalDateTime.now());
        activityRepository.save(activity);

        log.info("Terminated catchup session {} for user {}. Advanced lastViewed to {} and marked ACTIVE.",
                sessionId, userId, syncDeliveryId);

        return activityService.mapToSessionDto(session);
    }

    private TimelineDeliveryDto mapToTimelineDelivery(CollectedDelivery delivery, int[] progression) {
        return TimelineDeliveryDto.builder()
                .id(delivery.getId())
                .providerDeliveryId(delivery.getProviderDeliveryId())
                .innings(delivery.getInnings())
                .overNumber(delivery.getOverNumber())
                .ballNumber(delivery.getBallNumber())
                .batter(delivery.getBatter())
                .bowler(delivery.getBowler())
                .runs(delivery.getRuns())
                .extras(delivery.getExtras())
                .totalRuns(delivery.getTotalRuns())
                .inningsScoreAfterDelivery(progression[0])
                .inningsWicketsAfterDelivery(progression[1])
                .wicket(delivery.isWicket())
                .legalDelivery(delivery.isLegalDelivery())
                .wicketType(delivery.getWicketType())
                .eventType(delivery.getEventType())
                .eventDescription(delivery.getEventDescription())
                .collectedAt(delivery.getCollectedAt())
                .build();
    }

    private CatchupSession ownedSession(String sessionId, String userId) {
        CatchupSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found: " + sessionId));
        if (!session.getUserId().equals(userId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session does not belong to user");
        return session;
    }
}
