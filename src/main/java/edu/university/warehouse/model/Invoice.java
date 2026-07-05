package edu.university.warehouse.model;

import edu.university.warehouse.exception.DomainException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Invoice implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String invoiceId;
    private final InvoiceType type;
    private final LocalDateTime createdAt;
    private final String supplierId;
    private InvoiceStatus status;
    private final List<InvoiceItem> items = new ArrayList<>();

    public Invoice(String invoiceId, InvoiceType type, String supplierId) {
        this(invoiceId, type, supplierId, LocalDateTime.now(), InvoiceStatus.DRAFT);
    }

    public Invoice(String invoiceId, InvoiceType type, String supplierId, LocalDateTime createdAt, InvoiceStatus status, List<InvoiceItem> items) {
        this(invoiceId, type, supplierId, createdAt, status);
        if (items != null) {
            this.items.addAll(items);
        }
    }

    public Invoice(String invoiceId, InvoiceType type, String supplierId, LocalDateTime createdAt, InvoiceStatus status) {
        if (invoiceId == null || invoiceId.isBlank()) {
            throw new DomainException("Invoice ID cannot be blank");
        }
        if (type == null) {
            throw new DomainException("Invoice type cannot be null");
        }
        if (type == InvoiceType.INCOMING && (supplierId == null || supplierId.isBlank())) {
            throw new DomainException("Incoming invoice must have a supplier ID");
        }
        if (createdAt == null) {
            throw new DomainException("Created date cannot be null");
        }
        if (status == null) {
            throw new DomainException("Invoice status cannot be null");
        }

        this.invoiceId = invoiceId.trim().toUpperCase();
        this.type = type;
        this.createdAt = createdAt;
        this.supplierId = supplierId == null ? null : supplierId.trim().toUpperCase();
        this.status = status;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public InvoiceType getType() {
        return type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(InvoiceItem item) {
        if (status != InvoiceStatus.DRAFT) {
            throw new DomainException("Only draft invoice can be modified");
        }
        if (item == null) {
            throw new DomainException("Invoice item cannot be null");
        }
        items.add(item);
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
                .map(InvoiceItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void approve() {
        if (status != InvoiceStatus.DRAFT) {
            throw new DomainException("Only draft invoice can be approved");
        }
        if (items.isEmpty()) {
            throw new DomainException("Invoice must have at least one item");
        }
        status = InvoiceStatus.APPROVED;
    }

    public void cancel() {
        if (status == InvoiceStatus.APPROVED) {
            throw new DomainException("Approved invoice cannot be cancelled");
        }
        status = InvoiceStatus.CANCELLED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Invoice invoice)) {
            return false;
        }
        return Objects.equals(invoiceId, invoice.invoiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceId);
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId='" + invoiceId + '\'' +
                ", type=" + type +
                ", createdAt=" + createdAt +
                ", supplierId='" + supplierId + '\'' +
                ", status=" + status +
                ", itemsCount=" + items.size() +
                ", items=" + items +
                ", totalAmount=" + getTotalAmount() +
                '}';
    }
}
