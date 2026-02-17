package com.electrored.datapipeline.api.controller;

import com.electrored.datapipeline.api.model.TopSalesByCity;
import com.electrored.datapipeline.api.repository.TopSalesByCityRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/top-sales")
@Tag(name = "Top Sales by City", description = "APIs for querying top sales aggregated by city")
public class TopSalesByCityController {

    private final TopSalesByCityRepository repository;

    public TopSalesByCityController(TopSalesByCityRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/by-city")
    @Operation(summary = "Get top cities by sales", description = "Returns top cities ordered by total sales amount")
    public ResponseEntity<List<TopSalesByCity>> getTopCities(
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "10") int limit) {

        List<TopSalesByCity> results;

        if (country != null && !country.isEmpty()) {
            results = repository.findByCountryOrderByTotalSalesDesc(country);
        } else {
            results = repository.findTopCitiesByTotalSales();
        }

        // Apply limit
        results = results.stream()
                .limit(limit)
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/by-city/{city}")
    @Operation(summary = "Get sales for a specific city")
    public ResponseEntity<TopSalesByCity> getCityStats(@PathVariable String city) {
        return repository.findTopCitiesByTotalSales().stream()
                .filter(c -> c.getCity().equalsIgnoreCase(city))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

