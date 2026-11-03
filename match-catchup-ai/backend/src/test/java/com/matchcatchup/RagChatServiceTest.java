package com.matchcatchup.service;

import com.matchcatchup.entity.CollectedDelivery;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RagChatServiceTest {
    private final RagChatService service = new RagChatService(null, null, null, null);

    @Test void recognizesAnExactMultiDigitOverNumber() {
        assertEquals(17, service.parseRequestedOver("How many runs were scored in over 17?").orElseThrow());
        assertEquals(17, service.parseRequestedOver("What happened in the 17th over?").orElseThrow());
    }

    @Test void reportsAnOverSeparatelyForEachInnings() {
        List<CollectedDelivery> deliveries = List.of(
                delivery(1, 1, 1, "Bowler One", 2),
                delivery(1, 17, 1, "Bowler Two", 5),
                delivery(2, 17, 1, "Bowler Three", 12));

        String answer = service.answerForOver(17, deliveries);

        assertTrue(answer.contains("innings 1, over 17, 5 runs"));
        assertTrue(answer.contains("innings 2, over 17, 12 runs"));
        assertFalse(answer.contains("Bowler One"));
    }

    private CollectedDelivery delivery(int innings, int over, int ball, String bowler, int runs) {
        return CollectedDelivery.builder().matchId("IND-AUS-001").providerDeliveryId((long) (innings * 100 + over * 10 + ball))
                .innings(innings).overNumber(over).ballNumber(ball).bowler(bowler).totalRuns(runs).build();
    }
}
