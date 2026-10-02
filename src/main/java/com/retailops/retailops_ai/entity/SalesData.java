package com.retailops.retailops_ai.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "sales_data")
public class SalesData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "store_id")
    private String storeId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "category")
    private String category;

    @Column(name = "region")
    private String region;

    @Column(name = "inventory_level")
    private Integer inventoryLevel;

    @Column(name = "units_sold")
    private Integer unitsSold;

    @Column(name = "units_ordered")
    private Integer unitsOrdered;

    @Column(name = "price")
    private Double price;

    @Column(name = "discount")
    private Double discount;

    @Column(name = "promotion")
    private String promotion;

    @Column(name = "demand")
    private Integer demand;

    @Column(name = "weather_condition")
    private String weatherCondition;

    @Column(name = "seasonality")
    private String seasonality;

    @Column(name = "epidemic")
    private String epidemic;

    @Column(name = "competitor_pricing")
    private Double competitorPricing;


    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Integer getInventoryLevel() {
        return inventoryLevel;
    }

    public void setInventoryLevel(Integer inventoryLevel) {
        this.inventoryLevel = inventoryLevel;
    }

    public Integer getUnitsSold() {
        return unitsSold;
    }

    public void setUnitsSold(Integer unitsSold) {
        this.unitsSold = unitsSold;
    }

    public Integer getUnitsOrdered() {
        return unitsOrdered;
    }

    public void setUnitsOrdered(Integer unitsOrdered) {
        this.unitsOrdered = unitsOrdered;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getPromotion() {
        return promotion;
    }

    public void setPromotion(String promotion) {
        this.promotion = promotion;
    }

    public Integer getDemand() {
        return demand;
    }

    public void setDemand(Integer demand) {
        this.demand = demand;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getSeasonality() {
        return seasonality;
    }

    public void setSeasonality(String seasonality) {
        this.seasonality = seasonality;
    }

    public String getEpidemic() {
        return epidemic;
    }

    public void setEpidemic(String epidemic) {
        this.epidemic = epidemic;
    }

    public Double getCompetitorPricing() {
        return competitorPricing;
    }

    public void setCompetitorPricing(Double competitorPricing) {
        this.competitorPricing = competitorPricing;
    }
}