package com.matchcatchup.controller;

import com.matchcatchup.dto.CatchupSessionDto;
import com.matchcatchup.dto.TimelineResponseDto;
import com.matchcatchup.service.CatchupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CatchupController {

    private final CatchupService catchupService;

    @GetMapping("/api/matches/{matchId}/catchup")
    public CatchupSessionDto getAvailableCatchupSession(
            @PathVariable String matchId,
            Principal principal) {
        return catchupService.getAvailableCatchupSession(matchId, principal.getName());
    }

    @GetMapping("/api/catchup/{sessionId}")
    public CatchupSessionDto getSessionById(@PathVariable String sessionId, Principal principal) {
        return catchupService.getSessionById(sessionId, principal.getName());
    }

    @GetMapping("/api/catchup/{sessionId}/timeline")
    public TimelineResponseDto getSessionTimeline(@PathVariable String sessionId, Principal principal) {
        return catchupService.getTimelineForSession(sessionId, principal.getName());
    }

    @GetMapping("/api/catchup/{sessionId}/summary")
    public Map<String, String> getSessionSummary(@PathVariable String sessionId, Principal principal) {
        String summary = catchupService.getSessionSummary(sessionId, principal.getName());
        return Collections.singletonMap("summary", summary);
    }

    @PostMapping("/api/catchup/{sessionId}/complete")
    public CatchupSessionDto completeCatchup(
            @PathVariable String sessionId,
            Principal principal) {
        return catchupService.completeCatchup(sessionId, principal.getName());
    }
}
