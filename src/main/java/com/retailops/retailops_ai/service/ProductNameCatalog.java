package com.retailops.retailops_ai.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductNameCatalog {

    private final Map<String, String> clothingNames;

    public ProductNameCatalog() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("product-names.csv").getInputStream(), StandardCharsets.UTF_8))) {
            clothingNames = reader.lines()
                    .skip(1)
                    .map(line -> line.split(",", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toUnmodifiableMap(
                            parts -> parts[0].trim(),
                            parts -> parts[1].trim()));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load product-names.csv", exception);
        }
    }

    public String displayName(String productId, String category) {
        if ("Clothing".equalsIgnoreCase(category)) {
            return clothingNames.getOrDefault(productId, "Product " + productId);
        }
        return "Product " + productId;
    }
}