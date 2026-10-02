package com.retailops.retailops_ai.controller;

import com.retailops.retailops_ai.entity.SalesData;
import com.retailops.retailops_ai.repository.SalesDataRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesDataController {

    private final SalesDataRepository salesDataRepository;

    public SalesDataController(SalesDataRepository salesDataRepository) {
        this.salesDataRepository = salesDataRepository;
    }

    @GetMapping
    public List<SalesData> getAllSales(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        if (page < 0 || size < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be non-negative and size must be positive");
        }

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "date").and(Sort.by(Sort.Direction.DESC, "id"));
        return salesDataRepository.findAll(PageRequest.of(page, Math.min(size, 500), newestFirst)).getContent();
    }

    @GetMapping("/{id}")
    public SalesData getSaleById(@PathVariable Long id) {
        return salesDataRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sale not found"));
    }
}