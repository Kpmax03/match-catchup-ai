package com.match_score_generator.repository;

import com.match_score_generator.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery,Long> {
    List<Delivery> findByInningsAndIdLessThanEqualOrderByIdAsc(
            int innings, Long id
    );

    List<Delivery> findByInningsOrderByIdAsc(int innings);
}
