package com.matchcatchup;

import com.matchcatchup.dto.HeartbeatResponseDto;
import com.matchcatchup.entity.MatchCollectionState;
import com.matchcatchup.entity.CatchupSession;
import com.matchcatchup.entity.UserMatchActivity;
import com.matchcatchup.repository.CatchupSessionRepository;
import com.matchcatchup.repository.CollectedDeliveryRepository;
import com.matchcatchup.repository.MatchCollectionStateRepository;
import com.matchcatchup.repository.UserMatchActivityRepository;
import com.matchcatchup.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTest {

    @Mock
    private UserMatchActivityRepository activityRepository;

    @Mock
    private MatchCollectionStateRepository collectionStateRepository;

    @Mock
    private CollectedDeliveryRepository deliveryRepository;

    @Mock
    private CatchupSessionRepository catchupSessionRepository;

    @InjectMocks
    private ActivityService activityService;

    private final String matchId = "IND-AUS-001";
    private final String userId = "user-001";

    @Test
    void testHeartbeat_WhenUserReturnsFromInactive_CreatesCatchupSession() {
        // Given
        UserMatchActivity inactiveUser = UserMatchActivity.builder()
                .userId(userId)
                .matchId(matchId)
                .activityStatus("INACTIVE")
                .inactiveFrom(LocalDateTime.now().minusMinutes(5))
                .lastHeartbeatAt(LocalDateTime.now().minusMinutes(5))
                .lastViewedDeliveryId(32L)
                .build();

        MatchCollectionState state = MatchCollectionState.builder()
                .matchId(matchId)
                .lastCollectedDeliveryId(56L)
                .build();

        when(activityRepository.findByUserIdAndMatchId(userId, matchId)).thenReturn(Optional.of(inactiveUser));
        when(collectionStateRepository.findByMatchId(matchId)).thenReturn(Optional.of(state));
        when(catchupSessionRepository.findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtDesc(userId, matchId, "PENDING"))
                .thenReturn(Optional.empty());
        when(catchupSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        HeartbeatResponseDto response = activityService.recordHeartbeat(matchId, userId);

        // Then
        assertNotNull(response);
        assertEquals("ACTIVE", response.getActivityStatus());
        assertTrue(response.isCatchupAvailable());
        assertNotNull(response.getCatchupSession());
        assertEquals(33L, response.getCatchupSession().getFromDeliveryId());
        assertEquals(56L, response.getCatchupSession().getToDeliveryId());
        assertEquals(24, response.getCatchupSession().getMissedDeliveriesCount());
    }

    @Test
    void testHeartbeat_WhenActiveUser_UpdatesHeartbeatTimestamp() {
        // Given
        UserMatchActivity activeUser = UserMatchActivity.builder()
                .userId(userId)
                .matchId(matchId)
                .activityStatus("ACTIVE")
                .lastHeartbeatAt(LocalDateTime.now().minusSeconds(10))
                .lastViewedDeliveryId(32L)
                .build();

        MatchCollectionState state = MatchCollectionState.builder()
                .matchId(matchId)
                .lastCollectedDeliveryId(32L)
                .build();

        when(activityRepository.findByUserIdAndMatchId(userId, matchId)).thenReturn(Optional.of(activeUser));
        when(collectionStateRepository.findByMatchId(matchId)).thenReturn(Optional.of(state));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // When
        HeartbeatResponseDto response = activityService.recordHeartbeat(matchId, userId);

        // Then
        assertNotNull(response);
        assertEquals("ACTIVE", response.getActivityStatus());
        assertFalse(response.isCatchupAvailable());
    }

    @Test
    void testHeartbeat_KeepsViewingProgressIndependentAcrossUsers() {
        UserMatchActivity first = UserMatchActivity.builder().userId("fan-a").matchId(matchId)
                .activityStatus("ACTIVE").lastHeartbeatAt(LocalDateTime.now()).lastViewedDeliveryId(8L).build();
        UserMatchActivity second = UserMatchActivity.builder().userId("fan-b").matchId(matchId)
                .activityStatus("ACTIVE").lastHeartbeatAt(LocalDateTime.now()).lastViewedDeliveryId(21L).build();
        when(activityRepository.findByUserIdAndMatchId("fan-a", matchId)).thenReturn(Optional.of(first));
        when(activityRepository.findByUserIdAndMatchId("fan-b", matchId)).thenReturn(Optional.of(second));
        when(collectionStateRepository.findByMatchId(matchId)).thenReturn(Optional.of(MatchCollectionState.builder()
                .matchId(matchId).lastCollectedDeliveryId(25L).build()));
        when(catchupSessionRepository.findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(anyString(), eq(matchId), eq("PENDING")))
                .thenReturn(Optional.empty());
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HeartbeatResponseDto firstResponse = activityService.recordHeartbeat(matchId, "fan-a");
        HeartbeatResponseDto secondResponse = activityService.recordHeartbeat(matchId, "fan-b");

        assertEquals(8L, firstResponse.getLastViewedDeliveryId());
        assertEquals(21L, secondResponse.getLastViewedDeliveryId());
        verify(activityRepository).findByUserIdAndMatchId("fan-a", matchId);
        verify(activityRepository).findByUserIdAndMatchId("fan-b", matchId);
    }

    @Test
    void testHeartbeat_NeverExpandsAnExistingCatchupSession() {
        CatchupSession pending = CatchupSession.builder().id("old-session").userId(userId).matchId(matchId)
                .fromDeliveryId(5L).toDeliveryId(10L).status("PENDING").createdAt(LocalDateTime.now().minusMinutes(1)).build();
        UserMatchActivity inactive = UserMatchActivity.builder().userId(userId).matchId(matchId).activityStatus("INACTIVE")
                .inactiveFrom(LocalDateTime.now().minusSeconds(60)).lastHeartbeatAt(LocalDateTime.now().minusSeconds(60))
                .lastViewedDeliveryId(4L).build();
        when(activityRepository.findByUserIdAndMatchId(userId, matchId)).thenReturn(Optional.of(inactive));
        when(collectionStateRepository.findByMatchId(matchId)).thenReturn(Optional.of(MatchCollectionState.builder().matchId(matchId).lastCollectedDeliveryId(12L).build()));
        when(catchupSessionRepository.findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(userId, matchId, "PENDING")).thenReturn(Optional.of(pending));
        when(catchupSessionRepository.findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtDesc(userId, matchId, "PENDING")).thenReturn(Optional.of(pending));
        when(catchupSessionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(activityRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HeartbeatResponseDto response = activityService.recordHeartbeat(matchId, userId);

        assertEquals("old-session", response.getCatchupSession().getId());
        assertEquals(10L, response.getCatchupSession().getToDeliveryId());
        verify(catchupSessionRepository).save(argThat(session -> session.getFromDeliveryId() == 11L && session.getToDeliveryId() == 12L));
        assertEquals(10L, pending.getToDeliveryId());
    }
}
