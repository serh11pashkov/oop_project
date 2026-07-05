package edu.university.warehouse.persistence;

import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.model.Product;
import edu.university.warehouse.model.Supplier;

import java.io.Serializable;
import java.util.List;

public class WarehouseState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<Product> products;
    private final List<Supplier> suppliers;
    private final List<Invoice> invoices;

    public WarehouseState(List<Product> products, List<Supplier> suppliers, List<Invoice> invoices) {
        this.products = List.copyOf(products);
        this.suppliers = List.copyOf(suppliers);
        this.invoices = List.copyOf(invoices);
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<Supplier> getSuppliers() {
        return suppliers;
    }

    public List<Invoice> getInvoices() {
        return invoices;
    }
}