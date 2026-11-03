package com.matchcatchup.repository;

import com.matchcatchup.entity.CatchupSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatchupSessionRepository extends JpaRepository<CatchupSession, String> {

    List<CatchupSession> findByUserIdAndMatchIdOrderByCreatedAtDesc(String userId, String matchId);

    Optional<CatchupSession> findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtDesc(
            String userId, String matchId, String status
    );

    Optional<CatchupSession> findFirstByUserIdAndMatchIdAndStatusOrderByCreatedAtAsc(
            String userId, String matchId, String status
    );

    List<CatchupSession> findByUserIdAndMatchIdAndStatus(String userId, String matchId, String status);

    @Modifying
    @Transactional
    @Query("DELETE FROM CatchupSession s WHERE s.matchId = :matchId")
    void deleteByMatchId(@Param("matchId") String matchId);
}
