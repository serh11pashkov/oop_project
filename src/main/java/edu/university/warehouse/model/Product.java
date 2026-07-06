package edu.university.warehouse.model;

import edu.university.warehouse.exception.DomainException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.Serializable;
import java.util.Objects;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sku;
    private String name;
    private ProductCategory category;
    private BigDecimal unitPrice;
    private int quantityInStock;
    private int reorderLevel;

    public Product(String sku, String name, ProductCategory category, BigDecimal unitPrice, int initialQuantity, int reorderLevel) {
        if (sku == null || sku.isBlank()) {
            throw new DomainException("SKU cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Product name cannot be blank");
        }
        if (category == null) {
            throw new DomainException("Category cannot be null");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new DomainException("Unit price must be non-negative");
        }
        if (initialQuantity < 0) {
            throw new DomainException("Initial quantity cannot be negative");
        }
        if (reorderLevel < 0) {
            throw new DomainException("Reorder level cannot be negative");
        }

        this.sku = sku.trim().toUpperCase();
        this.name = name.trim();
        this.category = category;
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        this.quantityInStock = initialQuantity;
        this.reorderLevel = reorderLevel;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public int getReorderLevel() {
        return reorderLevel;
    }

    public void updateDetails(String name, ProductCategory category, BigDecimal unitPrice, int reorderLevel) {
        if (name == null || name.isBlank()) {
            throw new DomainException("Product name cannot be blank");
        }
        if (category == null) {
            throw new DomainException("Category cannot be null");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new DomainException("Unit price must be non-negative");
        }
        if (reorderLevel < 0) {
            throw new DomainException("Reorder level cannot be negative");
        }

        this.name = name.trim();
        this.category = category;
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
        this.reorderLevel = reorderLevel;
    }

    public void setQuantityInStock(int quantityInStock) {
        if (quantityInStock < 0) {
            throw new DomainException("Quantity in stock cannot be negative");
        }
        this.quantityInStock = quantityInStock;
    }

    public void increaseStock(int amount) {
        if (amount <= 0) {
            throw new DomainException("Stock increase amount must be positive");
        }
        this.quantityInStock += amount;
    }

    public void decreaseStock(int amount) {
        if (amount <= 0) {
            throw new DomainException("Stock decrease amount must be positive");
        }
        if (amount > this.quantityInStock) {
            throw new DomainException("Insufficient stock for SKU: " + sku);
        }
        this.quantityInStock -= amount;
    }

    public boolean needsReorder() {
        return quantityInStock <= reorderLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Product product)) {
            return false;
        }
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }

    @Override
    public String toString() {
        return "Product{" +
                "sku='" + sku + '\'' +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", unitPrice=" + unitPrice +
                ", quantityInStock=" + quantityInStock +
                ", reorderLevel=" + reorderLevel +
                '}';
    }
}
