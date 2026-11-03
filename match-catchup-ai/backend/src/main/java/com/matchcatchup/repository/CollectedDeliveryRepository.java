package com.matchcatchup.repository;

import com.matchcatchup.entity.CollectedDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectedDeliveryRepository extends JpaRepository<CollectedDelivery, Long> {

    Optional<CollectedDelivery> findByMatchIdAndProviderDeliveryId(String matchId, Long providerDeliveryId);

    boolean existsByMatchIdAndProviderDeliveryId(String matchId, Long providerDeliveryId);

    List<CollectedDelivery> findByMatchIdAndProviderDeliveryIdBetweenOrderByProviderDeliveryIdAsc(
            String matchId, Long fromDeliveryId, Long toDeliveryId
    );

    List<CollectedDelivery> findByMatchIdOrderByProviderDeliveryIdAsc(String matchId);

    List<CollectedDelivery> findTop30ByMatchIdOrderByProviderDeliveryIdDesc(String matchId);

    Optional<CollectedDelivery> findTopByMatchIdOrderByProviderDeliveryIdDesc(String matchId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CollectedDelivery d WHERE d.matchId = :matchId")
    void deleteByMatchId(@Param("matchId") String matchId);
}
