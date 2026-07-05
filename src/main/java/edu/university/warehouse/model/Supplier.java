package edu.university.warehouse.model;

import edu.university.warehouse.exception.DomainException;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Supplier implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String supplierId;
    private String name;
    private String email;
    private String phone;
    private final Set<String> suppliedSkus = new HashSet<>();

    public Supplier(String supplierId, String name, String email, String phone) {
        if (supplierId == null || supplierId.isBlank()) {
            throw new DomainException("Supplier ID cannot be blank");
        }
        if (name == null || name.isBlank()) {
            throw new DomainException("Supplier name cannot be blank");
        }

        this.supplierId = supplierId.trim().toUpperCase();
        this.name = name.trim();
        this.email = sanitize(email);
        this.phone = sanitize(phone);
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Set<String> getSuppliedSkus() {
        return Collections.unmodifiableSet(suppliedSkus);
    }

    public void updateContact(String email, String phone) {
        this.email = sanitize(email);
        this.phone = sanitize(phone);
    }

    public void assignSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new DomainException("SKU cannot be blank");
        }
        suppliedSkus.add(sku.trim().toUpperCase());
    }

    public boolean canSupply(String sku) {
        if (sku == null || sku.isBlank()) {
            return false;
        }
        return suppliedSkus.contains(sku.trim().toUpperCase());
    }

    private String sanitize(String input) {
        return input == null ? null : input.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Supplier supplier)) {
            return false;
        }
        return Objects.equals(supplierId, supplier.supplierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId);
    }

    @Override
    public String toString() {
        return "Supplier{" +
                "supplierId='" + supplierId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", suppliedSkus=" + suppliedSkus +
                '}';
    }
}
