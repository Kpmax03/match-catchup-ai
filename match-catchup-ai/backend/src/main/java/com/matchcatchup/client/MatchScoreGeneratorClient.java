package com.matchcatchup.client;

import com.matchcatchup.client.dto.ProviderLiveScoreDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "match-score-generator", url = "${matchcatchup.score-generator.url:http://localhost:8081}")
public interface MatchScoreGeneratorClient {

    @GetMapping("/live-score")
    ProviderLiveScoreDto getNextLiveScore(@RequestParam(name = "deliveryId", defaultValue = "0") Long deliveryId);
}
