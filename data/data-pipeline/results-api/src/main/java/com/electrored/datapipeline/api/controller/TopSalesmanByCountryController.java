package com.electrored.datapipeline.api.controller;

import com.electrored.datapipeline.api.model.TopSalesmanByCountry;
import com.electrored.datapipeline.api.repository.TopSalesmanByCountryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/top-salesmen")
@Tag(name = "Top Salesman by Country", description = "APIs for querying top salesmen aggregated by country")
public class TopSalesmanByCountryController {

    private final TopSalesmanByCountryRepository repository;

    public TopSalesmanByCountryController(TopSalesmanByCountryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/by-country")
    @Operation(summary = "Get top salesmen by country", description = "Returns top salesmen for a specific country or all countries")
    public ResponseEntity<List<TopSalesmanByCountry>> getTopSalesmen(
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "10") int limit) {

        List<TopSalesmanByCountry> results;

        if (country != null && !country.isEmpty()) {
            results = repository.findByCountryOrderByTotalSalesDesc(country);
        } else {
            results = repository.findTopSalesmenByTotalSales();
        }

        // Apply limit
        results = results.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/by-country/{sellerCode}")
    @Operation(summary = "Get stats for a specific salesman")
    public ResponseEntity<TopSalesmanByCountry> getSalesmanStats(@PathVariable String sellerCode) {
        return repository.findTopSalesmenByTotalSales().stream()
                .filter(s -> s.getSellerCode().equalsIgnoreCase(sellerCode))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

