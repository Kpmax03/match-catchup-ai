package com.matchcatchup.repository;

import com.matchcatchup.entity.UserMatchActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserMatchActivityRepository extends JpaRepository<UserMatchActivity, Long> {

    Optional<UserMatchActivity> findByUserIdAndMatchId(String userId, String matchId);

    List<UserMatchActivity> findByUserId(String userId);

    List<UserMatchActivity> findByActivityStatusAndLastHeartbeatAtBefore(String activityStatus, LocalDateTime threshold);

    @Modifying
    @Transactional
    @Query("UPDATE UserMatchActivity a SET a.activityStatus = 'INACTIVE', a.inactiveFrom = COALESCE(a.inactiveFrom, :now) WHERE a.userId = :userId")
    void markAllInactiveForUser(@Param("userId") String userId, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserMatchActivity a WHERE a.matchId = :matchId")
    void deleteByMatchId(@Param("matchId") String matchId);
}
