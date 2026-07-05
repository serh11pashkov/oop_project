package edu.university.warehouse.model;

import edu.university.warehouse.exception.DomainException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class InvoiceItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sku;
    private final int quantity;
    private final BigDecimal unitPrice;

    public InvoiceItem(String sku, int quantity, BigDecimal unitPrice) {
        if (sku == null || sku.isBlank()) {
            throw new DomainException("SKU cannot be blank");
        }
        if (quantity <= 0) {
            throw new DomainException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new DomainException("Unit price must be non-negative");
        }

        this.sku = sku.trim().toUpperCase();
        this.quantity = quantity;
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "sku='" + sku + '\'' +
                ", quantity=" + quantity +
                ", unitPrice=" + unitPrice +
                '}';
    }
}
