package com.matchcatchup;

import com.matchcatchup.client.MatchScoreGeneratorClient;
import com.matchcatchup.client.dto.ProviderDeliveryDto;
import com.matchcatchup.client.dto.ProviderLiveScoreDto;
import com.matchcatchup.entity.MatchCollectionState;
import com.matchcatchup.entity.MatchEntity;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import com.matchcatchup.repository.MatchCollectionStateRepository;
import com.matchcatchup.repository.MatchRepository;
import com.matchcatchup.service.CollectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private MatchScoreGeneratorClient scoreGeneratorClient;

    @Mock
    private MatchRepository matchRepository;

    @Mock
    private CollectedDeliveryRepository deliveryRepository;

    @Mock
    private MatchCollectionStateRepository collectionStateRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CollectionService collectionService;

    @Test
    void testCollectNextDelivery_AdvancesCursorAndPersists() {
        String matchId = "IND-AUS-001";
        MatchCollectionState state = MatchCollectionState.builder()
                .matchId(matchId)
                .lastCollectedDeliveryId(0L)
                .collectionStatus("COLLECTING")
                .build();

        ProviderDeliveryDto delivery = ProviderDeliveryDto.builder()
                .id(1L)
                .innings(1)
                .over(0)
                .ball(1)
                .batter("Rohit Sharma")
                .bowler("Mitchell Starc")
                .runs(0)
                .extras(0)
                .totalRuns(0)
                .wicket(false)
                .eventType("DOT")
                .eventDescription("Good length delivery outside off")
                .build();

        ProviderLiveScoreDto scoreDto = ProviderLiveScoreDto.builder()
                .matchId(matchId)
                .format("T20")
                .status("LIVE")
                .innings(1)
                .battingTeam("India")
                .bowlingTeam("Australia")
                .totalScore(0)
                .wickets(0)
                .overs(0)
                .balls(1)
                .runRate(0.0)
                .latestDelivery(delivery)
                .build();

        when(collectionStateRepository.findByMatchId(matchId)).thenReturn(Optional.of(state));
        when(scoreGeneratorClient.getNextLiveScore(0L)).thenReturn(scoreDto);
        when(deliveryRepository.existsByMatchIdAndProviderDeliveryId(matchId, 1L)).thenReturn(false);
        when(matchRepository.findById(matchId)).thenReturn(Optional.of(MatchEntity.builder().id(matchId).build()));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean success = collectionService.collectNextDelivery(matchId);

        assertTrue(success);
        verify(deliveryRepository, times(1)).save(any());
        verify(collectionStateRepository, times(1)).save(argThat(s -> s.getLastCollectedDeliveryId() == 1L));
        verify(valueOperations, times(1)).set(eq("match:IND-AUS-001:live"), any(), any(java.time.Duration.class));
    }
}
