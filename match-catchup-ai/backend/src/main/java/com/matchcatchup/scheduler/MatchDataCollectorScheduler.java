package com.matchcatchup.scheduler;

import com.matchcatchup.service.CollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchDataCollectorScheduler {

    private final CollectionService collectionService;

    @Value("${matchcatchup.collector.match-id:IND-AUS-001}")
    private String matchId;

    @Value("${matchcatchup.collector.reset-on-startup:true}")
    private boolean resetOnStartup;

    /**
     * Whenever the backend application runs/starts, resets match state and immediately collects
     * delivery ID 1 so the match starts from delivery ID 1 (1st ball of 1st over: 0.1 overs).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("MatchDataCollectorScheduler initialized for match {}", matchId);
        if (resetOnStartup) {
            try {
                collectionService.resetMatchToStart(matchId);
            } catch (Exception ex) {
                log.error("Failed to reset match state on startup: {}", ex.getMessage(), ex);
            }

            // Immediately collect delivery 1 so match starts at delivery ID 1 (1 ball of 1st over)
            boolean collected = false;
            for (int attempt = 1; attempt <= 5; attempt++) {
                try {
                    log.info("Startup collection attempt {} for delivery ID 1 on match {}", attempt, matchId);
                    collected = collectionService.collectNextDelivery(matchId);
                    if (collected) {
                        log.info("Match {} successfully started from delivery ID 1 (1 ball of 1st over)!", matchId);
                        break;
                    }
                } catch (Exception ex) {
                    log.warn("Startup collection attempt {} failed ({}). Retrying in 500ms...", attempt, ex.getMessage());
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {}
                }
            }
            if (!collected) {
                log.warn("Initial collection of delivery 1 deferred to scheduled run.");
            }
        }
    }

    /**
     * Scheduled collector running every 10 seconds.
     * Operates completely independently in the background.
     */
    @Scheduled(fixedDelayString = "${matchcatchup.collector.interval-ms:10000}", initialDelay = 10000)
    public void runCollector() {
        try {
            log.info("Scheduled Match Collector running for match {}", matchId);
            collectionService.collectNextDelivery(matchId);
        } catch (Exception ex) {
            log.error("Unhandled error in scheduled match data collector: {}", ex.getMessage(), ex);
        }
    }
}
