package com.matchcatchup.scheduler;

import com.matchcatchup.entity.UserMatchActivity;
import com.matchcatchup.repository.UserMatchActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InactivityDetectorScheduler {

    private final UserMatchActivityRepository activityRepository;

    @Value("${matchcatchup.activity.inactivity-threshold-seconds:45}")
    private long inactivityThresholdSeconds;

    /**
     * Periodically sweeps active user sessions to detect inactivity.
     * Rule: No heartbeat for 45 seconds = INACTIVE.
     */
    @Scheduled(fixedRateString = "${matchcatchup.activity.heartbeat-interval-ms:15000}", initialDelay = 15000)
    @Transactional
    public void detectInactivity() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(inactivityThresholdSeconds);

        List<UserMatchActivity> timedOutUsers = activityRepository
                .findByActivityStatusAndLastHeartbeatAtBefore("ACTIVE", threshold);

        for (UserMatchActivity activity : timedOutUsers) {
            log.info("Detected inactivity for user {} on match {} (last heartbeat: {})",
                    activity.getUserId(), activity.getMatchId(), activity.getLastHeartbeatAt());

            activity.setActivityStatus("INACTIVE");
            activity.setInactiveFrom(activity.getLastHeartbeatAt());
            activityRepository.save(activity);
        }
    }
}
