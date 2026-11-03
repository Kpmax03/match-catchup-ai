package com.matchcatchup.service;

import com.matchcatchup.dto.ActivityStatusResponseDto;
import com.matchcatchup.dto.CatchupSessionDto;
import com.matchcatchup.dto.HeartbeatResponseDto;
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
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {

    private final UserMatchActivityRepository activityRepository;
    private final MatchCollectionStateRepository collectionStateRepository;
    private final CollectedDeliveryRepository deliveryRepository;
    private final CatchupSessionRepository catchupSessionRepository;
    @Value("${matchcatchup.activity.inactivity-threshold-seconds:45}") private long inactivityThresholdSeconds = 45;

    /**
     * Records a heartbeat for user on match.
     * Detects return from inactivity and creates catch-up session if deliveries were missed.
     */
    @Transactional
    public HeartbeatResponseDto recordHeartbeat(String matchId, String userId) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Fetch or create user activity record
        UserMatchActivity activity = activityRepository.findByUserIdAndMatchId(userId, matchId)
                .orElseGet(() -> {
                    // When user first opens the match, initialize their lastViewedDeliveryId to current collected delivery id or 0
                    Long currentCollected = collectionStateRepository.findByMatchId(matchId)
                            .map(MatchCollectionState::getLastCollectedDeliveryId)
                            .orElse(0L);

                    UserMatchActivity newActivity = UserMatchActivity.builder()
                            .userId(userId)
                            .matchId(matchId)
                            .lastHeartbeatAt(now)
                            .activityStatus("ACTIVE")
                            .lastViewedDeliveryId(currentCollected)
                            .build();
                    return activityRepository.save(newActivity);
                });

        Long lastViewed = activity.getLastViewedDeliveryId() != null ? activity.getLastViewedDeliveryId() : 0L;
        Long latestCollected = collectionStateRepository.findByMatchId(matchId)
                .map(MatchCollectionState::getLastCollectedDeliveryId)
                .orElse(0L);

        // 2. Check if user already has an active pending catch-up session
        Optional<CatchupSession> pendingSession = catchupSessionRepository
                .findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(userId, matchId, "PENDING");

        boolean catchupAvailable = false;
        CatchupSessionDto sessionDto = null;

        if (pendingSession.isPresent()) {
            CatchupSession existing = pendingSession.get();
            // If user left again without finishing previous catch-up, extend toDeliveryId to latestCollected
            // so newly missed balls during the subsequent away period are seamlessly included!
            if (latestCollected > existing.getToDeliveryId()) {
                log.info("Extending pending catchup session {} for user {}: toDeliveryId from {} to {}",
                        existing.getId(), userId, existing.getToDeliveryId(), latestCollected);
                existing.setToDeliveryId(latestCollected);
                existing.setReturnedAt(now);
                existing.setSummary(null); // allow summary to recalculate with newly missed balls
                existing = catchupSessionRepository.save(existing);
            }

            catchupAvailable = true;
            sessionDto = mapToSessionDto(existing);
            log.info("User {} has pending catch-up session {} for match {}", userId, existing.getId(), matchId);
            activity.setActivityStatus("ACTIVE");
            activity.setInactiveFrom(null);
            activity.setLastHeartbeatAt(now);
            activityRepository.save(activity);

            return HeartbeatResponseDto.builder()
                    .userId(userId)
                    .matchId(matchId)
                    .activityStatus("ACTIVE")
                    .lastHeartbeatAt(now)
                    .lastViewedDeliveryId(lastViewed)
                    .latestCollectedDeliveryId(latestCollected)
                    .catchupAvailable(true)
                    .catchupSession(sessionDto)
                    .build();
        }

        // 3. User does NOT have an existing pending session.
        // Check if user was away / inactive (either marked INACTIVE, or heartbeat missed > 20s, or inactiveFrom set)
        boolean heartbeatExpired = activity.getLastHeartbeatAt() != null
                && activity.getLastHeartbeatAt().isBefore(now.minusSeconds(20));
        boolean wasAway = "INACTIVE".equalsIgnoreCase(activity.getActivityStatus())
                || heartbeatExpired
                || activity.getInactiveFrom() != null;

        if (wasAway) {
            log.info("User {} returned to match {} after being away/inactive! (lastHeartbeat: {}, status: {})",
                    userId, matchId, activity.getLastHeartbeatAt(), activity.getActivityStatus());

            LocalDateTime inactiveFrom = activity.getInactiveFrom() != null
                    ? activity.getInactiveFrom()
                    : (activity.getLastHeartbeatAt() != null ? activity.getLastHeartbeatAt() : now.minusSeconds(20));

            if (latestCollected > lastViewed) {
                Long fromDeliveryId = lastViewed + 1;
                Long toDeliveryId = latestCollected;

                CatchupSession session = CatchupSession.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .matchId(matchId)
                        .inactiveFrom(inactiveFrom)
                        .returnedAt(now)
                        .fromDeliveryId(fromDeliveryId)
                        .toDeliveryId(toDeliveryId)
                        .status("PENDING")
                        .createdAt(now)
                        .build();

                session = catchupSessionRepository.save(session);
                catchupAvailable = true;
                sessionDto = mapToSessionDto(session);
                log.info("Created catch-up session {} for user {}: missed deliveries {}-{}",
                        session.getId(), userId, fromDeliveryId, toDeliveryId);
            }

            // Restore active state
            activity.setActivityStatus("ACTIVE");
            activity.setInactiveFrom(null);
        } else {
            // User was actively watching live (no away interval and no pending catch-up)
            // Their viewing progress advances along with the live match
            if (latestCollected > lastViewed) {
                activity.setLastViewedDeliveryId(latestCollected);
            }
        }

        // 4. Update heartbeat timestamp and persist
        activity.setLastHeartbeatAt(now);
        activityRepository.save(activity);

        return HeartbeatResponseDto.builder()
                .userId(userId)
                .matchId(matchId)
                .activityStatus(activity.getActivityStatus())
                .lastHeartbeatAt(activity.getLastHeartbeatAt())
                .lastViewedDeliveryId(activity.getLastViewedDeliveryId())
                .latestCollectedDeliveryId(latestCollected)
                .catchupAvailable(catchupAvailable)
                .catchupSession(sessionDto)
                .build();
    }

    /**
     * Updates the user's last viewed delivery ID.
     */
    @Transactional
    public void recordViewedDelivery(String matchId, String userId, Long deliveryId) {
        if (deliveryId == null || deliveryId < 0) {
            return;
        }

        // If user has a pending catch-up session, do NOT advance viewing progress past fromDeliveryId
        Optional<CatchupSession> pendingSession = catchupSessionRepository
                .findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(userId, matchId, "PENDING");
        if (pendingSession.isPresent()) {
            log.debug("User {} has pending catch-up session {}, ignoring viewed delivery advancement to {}",
                    userId, pendingSession.get().getId(), deliveryId);
            return;
        }

        UserMatchActivity activity = activityRepository.findByUserIdAndMatchId(userId, matchId)
                .orElse(null);

        if (activity == null) {
            return;
        }

        // If user is currently marked INACTIVE or has missed heartbeats (> 20s),
        // do not let recordViewedDelivery overwrite viewing progress before heartbeat creates catch-up!
        boolean isAway = "INACTIVE".equalsIgnoreCase(activity.getActivityStatus())
                || activity.getLastHeartbeatAt() == null
                || activity.getLastHeartbeatAt().isBefore(LocalDateTime.now().minusSeconds(20));

        if (isAway) {
            log.info("Ignoring viewed delivery update because user {} was away on match {}", userId, matchId);
            return;
        }

        // Always update to the highest viewed delivery
        if (deliveryId > activity.getLastViewedDeliveryId()) {
            activity.setLastViewedDeliveryId(deliveryId);
            activityRepository.save(activity);
            log.info("Updated user {} lastViewedDeliveryId to {} for match {}", userId, deliveryId, matchId);
        }
    }

    /**
     * Marks the user as INACTIVE when they close the tab, switch away, or log out.
     */
    @Transactional
    public void recordUserLeave(String matchId, String userId) {
        UserMatchActivity activity = activityRepository.findByUserIdAndMatchId(userId, matchId)
                .orElse(null);
        if (activity != null) {
            activity.setActivityStatus("INACTIVE");
            activity.setInactiveFrom(LocalDateTime.now());
            activityRepository.save(activity);
            log.info("User {} marked INACTIVE on match {} due to tab close/leave/logout", userId, matchId);
        }
    }

    /**
     * Gets user activity status.
     */
    public ActivityStatusResponseDto getActivityStatus(String matchId, String userId) {
        UserMatchActivity activity = activityRepository.findByUserIdAndMatchId(userId, matchId)
                .orElseGet(() -> UserMatchActivity.builder()
                        .userId(userId)
                        .matchId(matchId)
                        .lastHeartbeatAt(LocalDateTime.now())
                        .activityStatus("ACTIVE")
                        .lastViewedDeliveryId(0L)
                        .build());

        Long latestCollected = collectionStateRepository.findByMatchId(matchId)
                .map(MatchCollectionState::getLastCollectedDeliveryId)
                .orElse(0L);

        Optional<CatchupSession> pendingSession = catchupSessionRepository
                .findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(userId, matchId, "PENDING");

        return ActivityStatusResponseDto.builder()
                .userId(userId)
                .matchId(matchId)
                .activityStatus(activity.getActivityStatus())
                .lastHeartbeatAt(activity.getLastHeartbeatAt())
                .inactiveFrom(activity.getInactiveFrom())
                .lastViewedDeliveryId(activity.getLastViewedDeliveryId())
                .latestCollectedDeliveryId(latestCollected)
                .catchupAvailable(pendingSession.isPresent())
                .activeCatchupSession(pendingSession.map(this::mapToSessionDto).orElse(null))
                .build();
    }

    public CatchupSessionDto mapToSessionDto(CatchupSession session) {
        int missedCount = (int) (session.getToDeliveryId() - session.getFromDeliveryId() + 1);
        if (missedCount < 0) missedCount = 0;

        String fromOverStr = formatOverBall(session.getMatchId(), session.getFromDeliveryId());
        String toOverStr = formatOverBall(session.getMatchId(), session.getToDeliveryId());

        return CatchupSessionDto.builder()
                .id(session.getId())
                .userId(session.getUserId())
                .matchId(session.getMatchId())
                .inactiveFrom(session.getInactiveFrom())
                .returnedAt(session.getReturnedAt())
                .fromDeliveryId(session.getFromDeliveryId())
                .toDeliveryId(session.getToDeliveryId())
                .missedDeliveriesCount(missedCount)
                .fromOver(fromOverStr)
                .toOver(toOverStr)
                .status(session.getStatus())
                .summary(session.getSummary())
                .createdAt(session.getCreatedAt())
                .build();
    }

    private String formatOverBall(String matchId, Long providerDeliveryId) {
        return deliveryRepository.findByMatchIdAndProviderDeliveryId(matchId, providerDeliveryId)
                .map(d -> d.getOverNumber() + "." + d.getBallNumber())
                .orElse("0.0");
    }
}
