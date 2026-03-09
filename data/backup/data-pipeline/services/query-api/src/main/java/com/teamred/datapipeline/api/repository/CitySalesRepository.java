package com.teamred.datapipeline.api.repository;

import com.teamred.datapipeline.api.entity.CitySales;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface CitySalesRepository extends JpaRepository<CitySales, Long> {

    @Query("SELECT c FROM CitySales c WHERE (:city IS NULL OR c.city = :city) AND c.windowStart >= :from AND c.windowStart <= :to ORDER BY c.windowStart DESC")
    Page<CitySales> findByCityAndDateRange(
            @Param("city") String city,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
