package com.match_score_generator.repository;

import com.match_score_generator.entity.LiveScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LiveScoreRepository extends JpaRepository<LiveScore,String> {
}
