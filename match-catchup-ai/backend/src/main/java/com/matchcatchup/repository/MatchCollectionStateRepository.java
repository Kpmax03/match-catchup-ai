package com.matchcatchup.repository;

import com.matchcatchup.entity.MatchCollectionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchCollectionStateRepository extends JpaRepository<MatchCollectionState, Long> {

    Optional<MatchCollectionState> findByMatchId(String matchId);
}
