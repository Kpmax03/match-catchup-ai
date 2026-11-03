package com.match_score_generator.controller;

import com.match_score_generator.dto.DeliveryResponseDto;
import com.match_score_generator.dto.LiveScoreResponseDto;
import com.match_score_generator.service.LiveScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ScoreController {

    @Autowired
    private LiveScoreService liveScoreService;

    @GetMapping("/live-score")
    public LiveScoreResponseDto getLiveScore(
            @RequestParam(defaultValue = "0") Long deliveryId) {

        return liveScoreService.getNextLiveScore("IND-AUS-001",deliveryId);
    }

}
