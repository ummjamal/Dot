package com.module.dot.model;

public class Item {

    private String globalID;
    private Long localID;
    private String creatorID;
    private String imagePath;
    private String name;
    private Double price;
    private String category;
    private String sku;
    private String unitType;
    private int stock;
    private double wholesalePrice;
    private Double tax;
    private String description;
    private Long quantity = 1L;

    public Item() {}

    public Item(
            String name,
            double price,
            String category,
            String sku,
            String unitType,
            int stock,
            double wholesalePrice,
            double tax,
            String description
    ) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.sku = sku;
        this.unitType = unitType;
        this.stock = stock;
        this.wholesalePrice = wholesalePrice;
        this.tax = tax;
        this.description = description;
    }

    public Item(
            long localID,
            String globalID,
            String imagePath,
            String name,
            double price,
            Double tax,
            String SKU,
            String unitType
    ) {
        this.localID = localID;
        this.globalID = globalID;
        this.imagePath = imagePath;
        this.name = name;
        this.price = price;
        this.tax = tax;
        this.sku = SKU;
        this.unitType = unitType;
    }

    public Item(
            String globalID,
            String name,
            double price,
            Double tax,
            String SKU,
            Long quantity
    ) {
        this.globalID = globalID;
        this.name = name;
        this.price = price;
        this.tax = tax;
        this.sku = SKU;
        this.quantity = quantity;
    }

    public Item(
            String globalID,
            String imagePath,
            String name,
            double price,
            Long quantity
    ) {
        this.globalID = globalID;
        this.imagePath = imagePath;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }

    public Item(String globalID, Double price, Long quantity) {
        this.globalID = globalID;
        this.price = price;
        this.quantity = quantity;
    }

    public String getGlobalID() {
        return globalID;
    }

    public void setGlobalID(String globalID) {
        this.globalID = globalID;
    }

    public Long getLocalID() {
        return localID;
    }

    public void setLocalID(Long localID) {
        this.localID = localID;
    }

    public String getCreatorID() {
        return creatorID;
    }

    public void setCreatorID(String creatorID) {
        this.creatorID = creatorID;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price == null ? 0.0 : price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSku() {
        return sku == null ? "" : sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getUnitType() {
        return unitType == null ? "" : unitType;
    }

    public void setUnitType(String unitType) {
        this.unitType = unitType;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = Math.max(stock, 0);
    }

    public double getWholesalePrice() {
        return wholesalePrice;
    }

    public void setWholesalePrice(double wholesalePrice) {
        this.wholesalePrice = Math.max(wholesalePrice, 0);
    }

    public double getTax() {
        return tax == null ? 0.0 : tax;
    }

    public void setTax(Double tax) {
        this.tax = tax == null ? 0.0 : tax;
    }

    public String getDescription() {
        return description == null ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getQuantity() {
        return quantity == null ? 1L : quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity == null || quantity < 1 ? 1L : quantity;
    }

    public boolean isAvailable() {
        return stock > 0;
    }

    public double getEstimatedProfitPerUnit() {
        return Math.max(0, getPrice() - wholesalePrice);
    }
}