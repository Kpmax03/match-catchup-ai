package com.match_score_generator.service;

import com.match_score_generator.dto.DeliveryResponseDto;
import com.match_score_generator.dto.LiveScoreResponseDto;
import com.match_score_generator.entity.Delivery;
import com.match_score_generator.entity.LiveScore;
import com.match_score_generator.repository.DeliveryRepository;
import com.match_score_generator.repository.LiveScoreRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveScoreService {

    private final DeliveryRepository deliveryRepository;
    private final LiveScoreRepository liveScoreRepository;

    @Transactional
    public LiveScoreResponseDto getNextLiveScore(
            String matchId, Long deliveryId) {

        if (deliveryId == null || deliveryId < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid delivery ID"
            );
        }

        // 1. Find the next delivery
        Long nextId = deliveryId + 1;

        Delivery delivery = deliveryRepository.findById(nextId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No more deliveries available"
                ));

        // 2. Fetch the existing live score
        LiveScore liveScore = liveScoreRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Match not found"
                ));

        int innings = delivery.getInnings();

        // 3. Get all deliveries of this innings up to the current ball
        List<Delivery> deliveries =
                deliveryRepository
                        .findByInningsAndIdLessThanEqualOrderByIdAsc(
                                innings, delivery.getId()
                        );

        // 4. Calculate current innings score
        int totalScore = deliveries.stream()
                .mapToInt(Delivery::getTotalRuns)
                .sum();

        int wickets = (int) deliveries.stream()
                .filter(Delivery::isWicket)
                .count();

        long legalBalls = deliveries.stream()
                .filter(Delivery::isLegalDelivery)
                .count();

        int overs = (int) (legalBalls / 6);
        int balls = (int) (legalBalls % 6);

        double runRate = legalBalls == 0
                ? 0.0
                : (double) totalScore / (legalBalls / 6.0);

        // 5. Update match information
        liveScore.setInnings(innings);
        liveScore.setTotalScore(totalScore);
        liveScore.setWickets(wickets);
        liveScore.setOvers(overs);
        liveScore.setBalls(balls);
        liveScore.setRunRate(
                Math.round(runRate * 100.0) / 100.0
        );

        if (innings == 1) {

            liveScore.setBattingTeam("India");
            liveScore.setBowlingTeam("Australia");
            liveScore.setTarget(null);

            if (legalBalls >= 120 || wickets >= 10) {
                liveScore.setStatus("INNINGS_BREAK");
            } else {
                liveScore.setStatus("LIVE");
            }

        } else {

            // Calculate target from India's actual first innings
            List<Delivery> firstInnings =
                    deliveryRepository.findByInningsOrderByIdAsc(1);

            int firstInningsScore = firstInnings.stream()
                    .mapToInt(Delivery::getTotalRuns)
                    .sum();

            int target = firstInningsScore + 1;

            liveScore.setBattingTeam("Australia");
            liveScore.setBowlingTeam("India");
            liveScore.setTarget(target);

            if (totalScore >= target ||
                    legalBalls >= 120 ||
                    wickets >= 10) {

                liveScore.setStatus("COMPLETED");

            } else {
                liveScore.setStatus("LIVE");
            }
        }

        // 6. Update timestamp and save
        liveScore.setLastUpdated(LocalDateTime.now());

        liveScoreRepository.save(liveScore);

        // 7. Convert latest delivery into DTO
        DeliveryResponseDto deliveryDto = DeliveryResponseDto.builder()
                .id(delivery.getId())
                .innings(delivery.getInnings())
                .over(delivery.getOverNumber())
                .ball(delivery.getBallNumber())
                .batter(delivery.getBatter())
                .bowler(delivery.getBowler())
                .runs(delivery.getRuns())
                .extras(delivery.getExtras())
                .totalRuns(delivery.getTotalRuns())
                .wicket(delivery.isWicket())
                .legalDelivery(delivery.isLegalDelivery())
                .wicketType(delivery.getWicketType())
                .eventType(delivery.getEventType())
                .eventDescription(delivery.getEventDescription())
                .build();

        // 8. Return updated live score
        return LiveScoreResponseDto.builder()
                .matchId(liveScore.getMatchId())
                .format(liveScore.getFormat())
                .status(liveScore.getStatus())
                .innings(liveScore.getInnings())
                .battingTeam(liveScore.getBattingTeam())
                .bowlingTeam(liveScore.getBowlingTeam())
                .totalScore(liveScore.getTotalScore())
                .wickets(liveScore.getWickets())
                .overs(liveScore.getOvers())
                .balls(liveScore.getBalls())
                .runRate(liveScore.getRunRate())
                .target(liveScore.getTarget())
                .latestDelivery(deliveryDto)
                .build();
    }
}
