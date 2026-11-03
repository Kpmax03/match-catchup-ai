package com.matchcatchup.service;

import com.matchcatchup.dto.ChatRequestDto;
import com.matchcatchup.dto.ChatResponseDto;
import com.matchcatchup.entity.CatchupSession;
import com.matchcatchup.entity.CollectedDelivery;
import com.matchcatchup.entity.MatchEntity;
import com.matchcatchup.repository.CatchupSessionRepository;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import com.matchcatchup.repository.MatchRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagChatService {

    private final CatchupSessionRepository sessionRepository;
    private final CollectedDeliveryRepository deliveryRepository;
    private final MatchRepository matchRepository;
    private final VectorStore vectorStore;
    private final ChatModel chatModel;

    /**
     * Match-level chat resolution.
     * If user has an active or past catch-up session for the match, delegates to it.
     * Otherwise, provides authoritative live match status answers.
     */
    public ChatResponseDto processMatchChat(String matchId, String userId, ChatRequestDto request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }

        // 1. Check if user has an active pending catch-up session
        Optional<CatchupSession> pendingSession = sessionRepository
                .findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(userId, matchId, "PENDING");
        if (pendingSession.isPresent()) {
            return processChat(pendingSession.get().getId(), userId, request);
        }

        // 2. Check if user has any completed catch-up session for this match
        List<CatchupSession> pastSessions = sessionRepository.findByUserIdAndMatchIdOrderByCreatedAtDesc(userId, matchId);
        if (!pastSessions.isEmpty()) {
            return processChat(pastSessions.get(0).getId(), userId, request);
        }

        // 3. If user has never had a catch-up session, answer based on live match
        MatchEntity match = matchRepository.findById(matchId).orElse(null);
        String q = request.getQuestion().toLowerCase();

        if (match != null) {
            if (q.contains("score") || q.contains("run") || q.contains("status")) {
                String ans = String.format("Live Match: %s vs %s. %s is batting at %d/%d in %d.%d overs (CRR: %.2f).%s",
                        match.getBattingTeam(), match.getBowlingTeam(), match.getBattingTeam(),
                        match.getTotalScore(), match.getWickets(), match.getOvers(), match.getBalls(), match.getRunRate(),
                        match.getTarget() != null ? " Target: " + match.getTarget() : "");
                return ChatResponseDto.builder().answer(ans).references(Collections.emptyList()).build();
            }
            if (q.contains("bat") || q.contains("batter") || q.contains("who is batting")) {
                return ChatResponseDto.builder()
                        .answer(String.format("%s is batting. Total score is %d/%d (%d.%d overs).",
                                match.getBattingTeam(), match.getTotalScore(), match.getWickets(), match.getOvers(), match.getBalls()))
                        .references(Collections.emptyList()).build();
            }
        }

        return ChatResponseDto.builder()
                .answer("You are currently watching the live match! You haven't stepped away yet. When you leave the match and return, I will answer questions strictly about the deliveries you missed.")
                .references(Collections.emptyList())
                .build();
    }

    /**
     * Answers questions strictly restricted to the missed deliveries of the catch-up session.
     */
    public ChatResponseDto processChat(String sessionId, String userId, ChatRequestDto request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question is required");
        }

        CatchupSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catch-up session not found: " + sessionId));

        // Validate session ownership
        if (!userId.equals(session.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Session belongs to another user");
        }

        // Retrieve ONLY deliveries belonging to this missed interval
        List<CollectedDelivery> missedDeliveries = deliveryRepository
                .findByMatchIdAndProviderDeliveryIdBetweenOrderByProviderDeliveryIdAsc(
                        session.getMatchId(), session.getFromDeliveryId(), session.getToDeliveryId()
                );

        if (missedDeliveries.isEmpty()) {
            return ChatResponseDto.builder()
                    .answer("No deliveries were recorded during this period.")
                    .references(Collections.emptyList())
                    .build();
        }

        // 1. MySQL is authoritative for counts, arithmetic, and factual match stats
        DeterministicStats stats = computeDeterministicStats(missedDeliveries);
        List<ChatResponseDto.ChatReferenceDto> references = findRelevantReferences(request.getQuestion(), missedDeliveries);

        // 2. Try deterministic match first
        String deterministicAnswer = answerDeterministically(request.getQuestion(), stats, missedDeliveries);
        if (deterministicAnswer != null && !deterministicAnswer.startsWith("I couldn't find")) {
            return ChatResponseDto.builder()
                    .answer(deterministicAnswer)
                    .references(references)
                    .build();
        }

        // 3. Index with VectorStore (if available) and query Ollama
        boolean vectorReady = session.isVectorIndexed();
        if (!vectorReady && vectorStore != null) {
            try {
                indexSession(session, missedDeliveries);
                vectorReady = true;
            } catch (Exception ex) {
                log.warn("VectorStore indexing unavailable for session {}: {}", sessionId, ex.getMessage());
            }
        }

        String answer = null;
        if (vectorReady && vectorStore != null && chatModel != null) {
            try {
                List<Document> context = vectorStore.similaritySearch(SearchRequest.builder()
                        .query(request.getQuestion()).topK(1000)
                        .build());
                List<Document> inRange = context.stream().filter(document -> {
                    Map<String, Object> metadata = document.getMetadata();
                    if (!userId.equals(metadata.get("userId")) || !sessionId.equals(metadata.get("catchupSessionId"))) return false;
                    Object raw = metadata.get("deliveryId");
                    if (!(raw instanceof Number)) return false;
                    long id = ((Number) raw).longValue();
                    return id >= session.getFromDeliveryId() && id <= session.getToDeliveryId();
                }).collect(Collectors.toList());

                if (!inRange.isEmpty()) {
                    String prompt = buildGroundedPrompt(request.getQuestion(), inRange, stats);
                    answer = chatModel.call(new Prompt(prompt)).getResult().getOutput().getText();
                }
            } catch (Exception ex) {
                log.warn("Spring AI retrieval or Ollama unavailable ({}).", ex.getMessage());
            }
        }

        // 4. Grounded factual fallback if Ollama/VectorStore is unavailable or produced empty text
        if (answer == null || answer.trim().isEmpty() || answer.startsWith("I couldn't find")) {
            answer = generateGeneralFallback(request.getQuestion(), stats, missedDeliveries);
        }

        return ChatResponseDto.builder()
                .answer(answer)
                .references(references)
                .build();
    }

    private DeterministicStats computeDeterministicStats(List<CollectedDelivery> deliveries) {
        int totalRuns = deliveries.stream().mapToInt(CollectedDelivery::getTotalRuns).sum();
        int totalWickets = (int) deliveries.stream().filter(CollectedDelivery::isWicket).count();
        int totalFours = (int) deliveries.stream().filter(d -> d.getRuns() == 4).count();
        int totalSixes = (int) deliveries.stream().filter(d -> d.getRuns() == 6).count();
        int dotBalls = (int) deliveries.stream().filter(d -> d.getTotalRuns() == 0).count();

        // Batter runs map
        Map<String, Integer> batterRuns = new LinkedHashMap<>();
        // Bowler wickets map
        Map<String, Integer> bowlerWickets = new LinkedHashMap<>();
        // Bowler runs conceded map
        Map<String, Integer> bowlerRuns = new LinkedHashMap<>();

        for (CollectedDelivery d : deliveries) {
            String b = d.getBatter() != null ? d.getBatter() : "Unknown";
            batterRuns.put(b, batterRuns.getOrDefault(b, 0) + d.getRuns());

            String bw = d.getBowler() != null ? d.getBowler() : "Unknown";
            bowlerRuns.put(bw, bowlerRuns.getOrDefault(bw, 0) + d.getTotalRuns());
            if (d.isWicket()) {
                bowlerWickets.put(bw, bowlerWickets.getOrDefault(bw, 0) + 1);
            }
        }

        return new DeterministicStats(
                deliveries.size(), totalRuns, totalWickets, totalFours, totalSixes, dotBalls,
                batterRuns, bowlerWickets, bowlerRuns
        );
    }

    private List<ChatResponseDto.ChatReferenceDto> findRelevantReferences(String question, List<CollectedDelivery> deliveries) {
        String qLower = question.toLowerCase();
        OptionalInt requestedOver = parseRequestedOver(question);
        List<ChatResponseDto.ChatReferenceDto> refs = new ArrayList<>();

        for (CollectedDelivery d : deliveries) {
            boolean matches = false;

            if ((qLower.contains("wicket") || qLower.contains("out")) && d.isWicket()) matches = true;
            if (qLower.contains("six") && d.getRuns() == 6) matches = true;
            if (qLower.contains("four") && d.getRuns() == 4) matches = true;
            if (d.getBatter() != null && qLower.contains(d.getBatter().toLowerCase())) matches = true;
            if (d.getBowler() != null && qLower.contains(d.getBowler().toLowerCase())) matches = true;
            if (requestedOver.isPresent() && requestedOver.getAsInt() == d.getOverNumber()) matches = true;

            if (matches) {
                refs.add(ChatResponseDto.ChatReferenceDto.builder()
                        .innings(d.getInnings())
                        .over(d.getOverNumber())
                        .ball(d.getBallNumber())
                        .description(String.format("%s to %s: %d runs (%s)",
                                d.getBowler(), d.getBatter(), d.getTotalRuns(),
                                d.isWicket() ? "WICKET: " + (d.getWicketType() != null ? d.getWicketType() : "OUT") : (d.getEventType() != null ? d.getEventType() : d.getTotalRuns() + " runs")))
                        .build());
            }
        }

        // If no direct matches, return key events (wickets & boundaries) or first deliveries
        if (refs.isEmpty()) {
            for (CollectedDelivery d : deliveries) {
                if (d.isWicket() || d.getRuns() >= 4) {
                    refs.add(ChatResponseDto.ChatReferenceDto.builder()
                            .innings(d.getInnings())
                            .over(d.getOverNumber())
                            .ball(d.getBallNumber())
                            .description(String.format("%s to %s: %d runs (%s)",
                                    d.getBowler(), d.getBatter(), d.getTotalRuns(),
                                    d.isWicket() ? "WICKET" : (d.getRuns() == 6 ? "SIX" : "FOUR")))
                            .build());
                }
                if (refs.size() >= 5) break;
            }
        }

        // Limit to top 5 most relevant references
        return refs.stream().limit(5).collect(Collectors.toList());
    }

    private void indexSession(CatchupSession session, List<CollectedDelivery> deliveries) {
        List<Document> documents = deliveries.stream().map(d -> new Document(
                session.getId() + "-" + d.getProviderDeliveryId(),
                String.format("Innings %d, over %d.%d: %s bowled to %s; batter runs %d, extras %d, total %d. Wicket: %s%s. Event: %s. %s",
                        d.getInnings(), d.getOverNumber(), d.getBallNumber(), d.getBowler(), d.getBatter(), d.getRuns(), d.getExtras(), d.getTotalRuns(), d.isWicket(),
                        d.getWicketType() == null ? "" : " (" + d.getWicketType() + ")", d.getEventType(), d.getEventDescription()),
                deliveryMetadata(session, d)))
                .collect(Collectors.toList());
        if (!documents.isEmpty()) vectorStore.add(documents);
        session.setVectorIndexed(true);
        sessionRepository.save(session);
    }

    private Map<String, Object> deliveryMetadata(CatchupSession session, CollectedDelivery delivery) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("matchId", session.getMatchId());
        metadata.put("userId", session.getUserId());
        metadata.put("catchupSessionId", session.getId());
        metadata.put("innings", delivery.getInnings());
        metadata.put("deliveryId", delivery.getProviderDeliveryId());
        metadata.put("overNumber", delivery.getOverNumber());
        metadata.put("ballNumber", delivery.getBallNumber());
        return metadata;
    }

    OptionalInt parseRequestedOver(String question) {
        Matcher matcher = Pattern.compile("\\bover\\s+(\\d{1,2})\\b|\\b(\\d{1,2})(?:st|nd|rd|th)\\s+over\\b", Pattern.CASE_INSENSITIVE)
                .matcher(question);
        if (!matcher.find()) return OptionalInt.empty();
        String number = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return OptionalInt.of(Integer.parseInt(number));
    }

    private String buildGroundedPrompt(String question, List<Document> documents, DeterministicStats stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are MatchCatchup AI's cricket assistant. Your job is to answer the user's question ");
        sb.append("using ONLY the ball-by-ball deliveries they missed while away.\n\n");
        sb.append("STRICT ISOLATION RULES:\n");
        sb.append("1. Answer ONLY based on the facts and statistics provided below.\n");
        sb.append("2. If the question asks about something outside these deliveries or something not in the data, ");
        sb.append("you MUST respond with: 'I couldn't find that information in the deliveries you missed.'\n");
        sb.append("3. Always cite the exact over and ball number (e.g. over 6.2) when referencing events.\n");
        sb.append("4. Use the exact arithmetic stats provided.\n\n");

        sb.append("VERIFIED STATS FOR THE MISSED INTERVAL:\n");
        sb.append(String.format("- Total deliveries: %d\n", stats.totalDeliveries));
        sb.append(String.format("- Total runs scored: %d\n", stats.totalRuns));
        sb.append(String.format("- Total wickets lost: %d\n", stats.totalWickets));
        sb.append(String.format("- Boundaries: %d sixes, %d fours\n", stats.totalSixes, stats.totalFours));
        sb.append(String.format("- Dot balls: %d\n", stats.dotBalls));
        sb.append("- Batter runs in this interval:\n");
        stats.batterRuns.forEach((b, r) -> sb.append(String.format("  * %s: %d runs\n", b, r)));
        sb.append("- Bowler stats in this interval:\n");
        stats.bowlerRuns.forEach((bw, r) -> sb.append(String.format("  * %s: %d runs, %d wickets\n",
                bw, r, stats.bowlerWickets.getOrDefault(bw, 0))));

        sb.append("\nBALL-BY-BALL DELIVERIES IN THIS MISSED INTERVAL:\n");
        for (Document document : documents) sb.append("- ").append(document.getText()).append("\n");

        sb.append("\nUSER QUESTION: ").append(question).append("\n");
        sb.append("ANSWER: ");

        return sb.toString();
    }

    private String answerDeterministically(String question, DeterministicStats stats, List<CollectedDelivery> deliveries) {
        String q = question.toLowerCase();

        // 1. Most runs / top scorer / highest scorer
        if (q.contains("most run") || q.contains("highest scorer") || q.contains("top scorer") || q.contains("who scored the most")) {
            Map.Entry<String, Integer> top = stats.batterRuns.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);
            if (top != null) {
                return String.format("Based on the deliveries you missed, %s scored the most runs with %d runs.",
                        top.getKey(), top.getValue());
            }
        }

        // 2. Which bowler took the wicket / who took wickets / wickets fell
        if (q.contains("which bowler") || (q.contains("who") && (q.contains("wicket") || q.contains("out")))) {
            List<CollectedDelivery> wickets = deliveries.stream()
                    .filter(CollectedDelivery::isWicket)
                    .collect(Collectors.toList());
            if (wickets.isEmpty()) {
                return "No wickets fell during the deliveries you missed.";
            }
            StringBuilder sb = new StringBuilder("Wickets taken while you were away:\n");
            for (CollectedDelivery w : wickets) {
                sb.append(String.format("- In over %d.%d, %s took the wicket of %s (%s).\n",
                        w.getOverNumber(), w.getBallNumber(), w.getBowler(), w.getBatter(),
                        w.getWicketType() != null ? w.getWicketType() : "OUT"));
            }
            return sb.toString().trim();
        }

        // 3. How many wickets fell
        if (q.contains("wicket") && (q.contains("how many") || q.contains("number") || q.contains("any wicket") || q.contains("fell") || q.contains("lost"))) {
            if (stats.totalWickets == 0) {
                return "No wickets fell during the deliveries you missed.";
            }
            return String.format("A total of %d wicket%s fell during the deliveries you missed.",
                    stats.totalWickets, (stats.totalWickets == 1 ? "" : "s"));
        }

        // 4. How many runs / total score in missed period
        if ((q.contains("run") || q.contains("score")) && (q.contains("how many") || q.contains("total") || q.contains("how much"))) {
            return String.format("A total of %d runs were scored in the %d deliveries you missed.", stats.totalRuns, stats.totalDeliveries);
        }

        // 5. How many deliveries / balls missed
        if ((q.contains("delivery") || q.contains("ball")) && (q.contains("how many") || q.contains("number") || q.contains("miss"))) {
            return String.format("You missed %d deliveries.", stats.totalDeliveries);
        }

        // 6. Sixes hit
        if (q.contains("six") || q.contains("sixes")) {
            return String.format("There %s %d six%s hit while you were away.",
                    (stats.totalSixes == 1 ? "was" : "were"), stats.totalSixes, (stats.totalSixes == 1 ? "" : "es"));
        }

        // 7. Fours / Boundaries
        if (q.contains("four") || q.contains("fours") || q.contains("boundar")) {
            return String.format("During the missed period, %d boundary four%s and %d six%s were hit (total runs: %d).",
                    stats.totalFours, (stats.totalFours == 1 ? "" : "s"),
                    stats.totalSixes, (stats.totalSixes == 1 ? "" : "es"),
                    stats.totalRuns);
        }

        // 8. Economical over / Economy
        if (q.contains("economical") || q.contains("economy")) {
            Map<String, EconomySpell> spells = new HashMap<>();
            for (CollectedDelivery delivery : deliveries) {
                String key = delivery.getInnings() + ":" + delivery.getOverNumber() + ":" + delivery.getBowler();
                EconomySpell spell = spells.computeIfAbsent(key, ignored -> new EconomySpell(delivery.getInnings(), delivery.getOverNumber(), delivery.getBowler()));
                spell.runsConceded += delivery.getTotalRuns();
                if (delivery.isLegalDelivery()) spell.legalBalls++;
            }
            EconomySpell best = spells.values().stream().filter(spell -> spell.legalBalls > 0)
                    .min(Comparator.comparingDouble(EconomySpell::economy)).orElse(null);
            if (best != null) return String.format("%s bowled the most economical over in innings %d (over %d), conceding %d runs from %d legal balls (economy %.2f).",
                    best.bowler, best.innings, best.over, best.runsConceded, best.legalBalls, best.economy());
        }

        // 9. Specific over question (e.g. "What happened in the 7th over?")
        OptionalInt requestedOver = parseRequestedOver(question);
        if (requestedOver.isPresent()) {
            return answerForOver(requestedOver.getAsInt(), deliveries);
        }

        // 10. Summary / what happened / highlights / tell me
        if (q.contains("summary") || q.contains("what happened") || q.contains("tell me") || q.contains("highlights") || q.contains("recap")) {
            return generateGeneralFallback(question, stats, deliveries);
        }

        // 11. Batters / who batted
        if (q.contains("batter") || q.contains("batsman") || q.contains("batters") || q.contains("who batted")) {
            if (stats.batterRuns.isEmpty()) {
                return "No batters scored runs during the deliveries you missed.";
            }
            StringBuilder sb = new StringBuilder("Batters who scored runs while you were away:\n");
            stats.batterRuns.forEach((b, r) -> sb.append(String.format("- %s: %d runs\n", b, r)));
            return sb.toString().trim();
        }

        // 12. Bowlers / who bowled
        if (q.contains("bowler") || q.contains("bowlers") || q.contains("who bowled")) {
            if (stats.bowlerRuns.isEmpty()) {
                return "No bowling figures recorded during the deliveries you missed.";
            }
            StringBuilder sb = new StringBuilder("Bowlers who bowled while you were away:\n");
            stats.bowlerRuns.forEach((bw, r) -> sb.append(String.format("- %s: %d runs conceded, %d wickets\n",
                    bw, r, stats.bowlerWickets.getOrDefault(bw, 0))));
            return sb.toString().trim();
        }

        return "I couldn't find that information in the deliveries you missed.";
    }

    String answerForOver(int targetOver, List<CollectedDelivery> deliveries) {
        List<CollectedDelivery> overDeliveries = deliveries.stream()
                .filter(d -> d.getOverNumber() == targetOver).collect(Collectors.toList());
        if (overDeliveries.isEmpty()) return String.format("I couldn't find delivery data for over %d in the deliveries you missed.", targetOver);
        Map<Integer, List<CollectedDelivery>> innings = overDeliveries.stream()
                .collect(Collectors.groupingBy(CollectedDelivery::getInnings, TreeMap::new, Collectors.toList()));
        StringBuilder answer = new StringBuilder();
        innings.forEach((inningsNumber, balls) -> {
            int runs = balls.stream().mapToInt(CollectedDelivery::getTotalRuns).sum();
            long wickets = balls.stream().filter(CollectedDelivery::isWicket).count();
            String bowler = balls.get(0).getBowler();
            answer.append(String.format("In innings %d, over %d, %d runs were scored and %d wicket%s fell (bowler: %s). ",
                    inningsNumber, targetOver, runs, wickets, wickets == 1 ? "" : "s", bowler));
        });
        return answer.toString().trim();
    }

    private String generateGeneralFallback(String question, DeterministicStats stats, List<CollectedDelivery> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            return "No deliveries were recorded during this period.";
        }
        CollectedDelivery first = deliveries.get(0);
        CollectedDelivery last = deliveries.get(deliveries.size() - 1);
        Map.Entry<String, Integer> top = stats.batterRuns.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
        String topBatter = top != null ? String.format("Top scorer was %s (%d runs). ", top.getKey(), top.getValue()) : "";

        return String.format("During the %d deliveries you missed (from over %d.%d to %d.%d), a total of %d runs were scored and %d wicket%s fell (%d sixes, %d fours). %s",
                stats.totalDeliveries,
                first.getOverNumber(), first.getBallNumber(),
                last.getOverNumber(), last.getBallNumber(),
                stats.totalRuns, stats.totalWickets, (stats.totalWickets == 1 ? " was" : "s were"),
                stats.totalSixes, stats.totalFours, topBatter);
    }

    private static class DeterministicStats {
        final int totalDeliveries;
        final int totalRuns;
        final int totalWickets;
        final int totalFours;
        final int totalSixes;
        final int dotBalls;
        final Map<String, Integer> batterRuns;
        final Map<String, Integer> bowlerWickets;
        final Map<String, Integer> bowlerRuns;

        DeterministicStats(int totalDeliveries, int totalRuns, int totalWickets, int totalFours, int totalSixes, int dotBalls,
                           Map<String, Integer> batterRuns, Map<String, Integer> bowlerWickets, Map<String, Integer> bowlerRuns) {
            this.totalDeliveries = totalDeliveries;
            this.totalRuns = totalRuns;
            this.totalWickets = totalWickets;
            this.totalFours = totalFours;
            this.totalSixes = totalSixes;
            this.dotBalls = dotBalls;
            this.batterRuns = batterRuns;
            this.bowlerWickets = bowlerWickets;
            this.bowlerRuns = bowlerRuns;
        }
    }

    private static class EconomySpell {
        final int innings;
        final int over;
        final String bowler;
        int runsConceded;
        int legalBalls;
        EconomySpell(int innings, int over, String bowler) { this.innings = innings; this.over = over; this.bowler = bowler; }
        double economy() { return runsConceded * 6.0 / legalBalls; }
    }
}
