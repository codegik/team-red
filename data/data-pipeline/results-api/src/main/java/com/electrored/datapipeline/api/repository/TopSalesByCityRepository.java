package com.electrored.datapipeline.api.repository;

import com.electrored.datapipeline.api.model.TopSalesByCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopSalesByCityRepository extends JpaRepository<TopSalesByCity, Long> {

    @Query("SELECT t FROM TopSalesByCity t ORDER BY t.totalSales DESC")
    List<TopSalesByCity> findTopCitiesByTotalSales();

    List<TopSalesByCity> findByCountryOrderByTotalSalesDesc(String country);
}

