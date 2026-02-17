package com.electrored.datapipeline.api.repository;

import com.electrored.datapipeline.api.model.TopSalesmanByCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopSalesmanByCountryRepository extends JpaRepository<TopSalesmanByCountry, Long> {

    List<TopSalesmanByCountry> findByCountryOrderByTotalSalesDesc(String country);

    @Query("SELECT t FROM TopSalesmanByCountry t ORDER BY t.totalSales DESC")
    List<TopSalesmanByCountry> findTopSalesmenByTotalSales();
}

