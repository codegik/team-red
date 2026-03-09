package com.teamred.datapipeline.api.repository;

import com.teamred.datapipeline.api.entity.SalesmanStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface SalesmanStatsRepository extends JpaRepository<SalesmanStats, Long> {

    @Query("SELECT s FROM SalesmanStats s WHERE s.windowStart >= :from AND s.windowStart <= :to ORDER BY s.totalSales DESC")
    Page<SalesmanStats> findTopSalesmenByDateRange(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
