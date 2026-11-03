package com.matchcatchup.service;

import com.matchcatchup.entity.CatchupSession;
import com.matchcatchup.entity.CollectedDelivery;
import com.matchcatchup.repository.CatchupSessionRepository;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryService {

    private final CatchupSessionRepository sessionRepository;
    private final CollectedDeliveryRepository deliveryRepository;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.1:8b}")
    private String ollamaModel;

    private final RestTemplate restTemplate = createFastRestTemplate();

    private static RestTemplate createFastRestTemplate() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(4000);
        return new RestTemplate(factory);
    }

    /**
     * Retrieves existing cached summary or generates and persists a new one.
     */
    @Transactional
    public String getOrGenerateSummary(CatchupSession session) {
        if (session.getSummary() != null && !session.getSummary().trim().isEmpty()) {
            return session.getSummary();
        }

        List<CollectedDelivery> deliveries = deliveryRepository
                .findByMatchIdAndProviderDeliveryIdBetweenOrderByProviderDeliveryIdAsc(
                        session.getMatchId(), session.getFromDeliveryId(), session.getToDeliveryId()
                );

        if (deliveries.isEmpty()) {
            String emptySummary = "No deliveries were recorded during your away period.";
            session.setSummary(emptySummary);
            sessionRepository.save(session);
            return emptySummary;
        }

        String summaryText = generateFactualSummary(session, deliveries);

        // Optionally refine with Ollama if running
        try {
            String enhancedSummary = enhanceWithOllama(summaryText, deliveries);
            if (enhancedSummary != null && !enhancedSummary.trim().isEmpty()) {
                summaryText = enhancedSummary;
            }
        } catch (Exception ex) {
            log.warn("Ollama AI enhancement skipped or unavailable: {}. Using factual summary.", ex.getMessage());
        }

        session.setSummary(summaryText);
        sessionRepository.save(session);
        return summaryText;
    }

    private String generateFactualSummary(CatchupSession session, List<CollectedDelivery> deliveries) {
        int count = deliveries.size();
        int totalRuns = deliveries.stream().mapToInt(CollectedDelivery::getTotalRuns).sum();
        long wickets = deliveries.stream().filter(CollectedDelivery::isWicket).count();

        CollectedDelivery first = deliveries.get(0);
        CollectedDelivery last = deliveries.get(deliveries.size() - 1);

        String fromOverStr = first.getOverNumber() + "." + first.getBallNumber();
        String toOverStr = last.getOverNumber() + "." + last.getBallNumber();

        // Boundaries and key events
        List<CollectedDelivery> sixes = deliveries.stream()
                .filter(d -> d.getRuns() == 6)
                .collect(Collectors.toList());

        List<CollectedDelivery> fours = deliveries.stream()
                .filter(d -> d.getRuns() == 4)
                .collect(Collectors.toList());

        List<CollectedDelivery> wicketDeliveries = deliveries.stream()
                .filter(CollectedDelivery::isWicket)
                .collect(Collectors.toList());

        // Top run scorer in this window
        Map<String, Integer> batterRuns = deliveries.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getBatter() != null ? d.getBatter() : "Unknown",
                        Collectors.summingInt(CollectedDelivery::getRuns)
                ));

        String topBatter = batterRuns.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + " runs)")
                .orElse("N/A");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("You were away from over %s to %s, missing %d deliveries.\n\n", fromOverStr, toOverStr, count));
        sb.append(String.format("During this period, %d runs were scored and %d wicket%s lost.\n\n",
                totalRuns, wickets, (wickets == 1 ? " was" : "s were")));

        sb.append("Key Moments:\n");
        if (!wicketDeliveries.isEmpty()) {
            for (CollectedDelivery w : wicketDeliveries) {
                sb.append(String.format("- Wicket in over %d.%d: %s dismissed by %s (%s)\n",
                        w.getOverNumber(), w.getBallNumber(), w.getBatter(), w.getBowler(),
                        w.getWicketType() != null ? w.getWicketType() : "OUT"));
            }
        } else {
            sb.append("- The batting side kept all wickets intact.\n");
        }

        if (!sixes.isEmpty()) {
            sb.append(String.format("- %d six%s hit, including by %s (over %d.%d).\n",
                    sixes.size(), (sixes.size() == 1 ? " was" : "es were"),
                    sixes.get(0).getBatter(), sixes.get(0).getOverNumber(), sixes.get(0).getBallNumber()));
        }

        if (!fours.isEmpty()) {
            sb.append(String.format("- %d boundary four%s scored across the spell.\n",
                    fours.size(), (fours.size() == 1 ? " was" : "s were")));
        }

        sb.append(String.format("- Top scorer in this window: %s.\n\n", topBatter));
        sb.append(String.format("Current situation at your return: Match progressed to over %s.", toOverStr));

        return sb.toString();
    }

    private String enhanceWithOllama(String factualDraft, List<CollectedDelivery> deliveries) {
        String prompt = "You are an expert cricket commentator for MatchCatchup AI. "
                + "Here is the exact factual data of deliveries missed by a fan who just returned to the match:\n\n"
                + factualDraft + "\n\n"
                + "Please rewrite this into an engaging, clear, and professional catch-up summary in 3-4 bullet points. "
                + "CRITICAL RULE: Do NOT invent any extra stats, boundaries, player names, or events not mentioned in the factual data. "
                + "Stick 100% to the provided facts.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("prompt", prompt);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                ollamaBaseUrl + "/api/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Object resp = response.getBody().get("response");
            if (resp != null) {
                return resp.toString().trim();
            }
        }
        return factualDraft;
    }
}
