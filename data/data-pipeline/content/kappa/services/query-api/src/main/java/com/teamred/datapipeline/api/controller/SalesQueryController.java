package com.teamred.datapipeline.api.controller;

import com.teamred.datapipeline.api.entity.CitySales;
import com.teamred.datapipeline.api.entity.DataLineage;
import com.teamred.datapipeline.api.entity.SalesmanStats;
import com.teamred.datapipeline.api.repository.CitySalesRepository;
import com.teamred.datapipeline.api.repository.DataLineageRepository;
import com.teamred.datapipeline.api.repository.SalesmanStatsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SalesQueryController {

    private final CitySalesRepository citySalesRepository;
    private final SalesmanStatsRepository salesmanStatsRepository;
    private final DataLineageRepository dataLineageRepository;

    public SalesQueryController(CitySalesRepository citySalesRepository,
                                SalesmanStatsRepository salesmanStatsRepository,
                                DataLineageRepository dataLineageRepository) {
        this.citySalesRepository = citySalesRepository;
        this.salesmanStatsRepository = salesmanStatsRepository;
        this.dataLineageRepository = dataLineageRepository;
    }

    @GetMapping("/sales/by-city")
    public ResponseEntity<Page<CitySales>> getSalesByCity(
            @RequestParam(required = false) String city,
            @RequestParam(required = false, defaultValue = "1970-01-01T00:00:00Z") String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Instant fromInstant = Instant.parse(from);
        Instant toInstant = to != null ? Instant.parse(to) : Instant.now();

        Pageable pageable = PageRequest.of(page, size);
        Page<CitySales> result = citySalesRepository.findByCityAndDateRange(city, fromInstant, toInstant, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/sales/top-salesman")
    public ResponseEntity<Page<SalesmanStats>> getTopSalesmen(
            @RequestParam(required = false, defaultValue = "1970-01-01T00:00:00Z") String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit) {

        Instant fromInstant = Instant.parse(from);
        Instant toInstant = to != null ? Instant.parse(to) : Instant.now();

        Pageable pageable = PageRequest.of(0, limit);
        Page<SalesmanStats> result = salesmanStatsRepository.findTopSalesmenByDateRange(fromInstant, toInstant, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/lineage/{saleId}")
    public ResponseEntity<List<DataLineage>> getLineage(@PathVariable String saleId) {
        List<DataLineage> lineage = dataLineageRepository.findBySaleId(saleId);
        if (lineage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lineage);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "query-api",
                "timestamp", Instant.now().toString()
        ));
    }
}
