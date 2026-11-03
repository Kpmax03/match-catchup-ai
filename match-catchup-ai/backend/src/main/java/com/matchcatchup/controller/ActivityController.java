package com.matchcatchup.controller;

import com.matchcatchup.dto.ActivityStatusResponseDto;
import com.matchcatchup.dto.HeartbeatRequestDto;
import com.matchcatchup.dto.HeartbeatResponseDto;
import com.matchcatchup.dto.ViewedDeliveryRequestDto;
import com.matchcatchup.service.ActivityService;
import com.matchcatchup.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/matches/{matchId}/activity")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ActivityController {

    private final ActivityService activityService;
    private final AuthService authService;

    @PostMapping("/heartbeat")
    public HeartbeatResponseDto recordHeartbeat(
            @PathVariable String matchId,
            Principal principal) {
        authService.touch(principal.getName());
        return activityService.recordHeartbeat(matchId, principal.getName());
    }

    @PostMapping("/viewed")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordViewedDelivery(
            @PathVariable String matchId,
            @RequestBody ViewedDeliveryRequestDto request, Principal principal) {
        if (request != null && request.getDeliveryId() != null) activityService.recordViewedDelivery(matchId, principal.getName(), request.getDeliveryId());
    }

    @PostMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordUserLeave(
            @PathVariable String matchId,
            Principal principal) {
        if (principal != null) {
            activityService.recordUserLeave(matchId, principal.getName());
        }
    }

    @GetMapping("/status")
    public ActivityStatusResponseDto getActivityStatus(
            @PathVariable String matchId,
            Principal principal) {
        return activityService.getActivityStatus(matchId, principal.getName());
    }
}
