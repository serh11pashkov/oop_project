package edu.university.warehouse.service;

import edu.university.warehouse.exception.DomainException;
import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.model.InvoiceItem;
import edu.university.warehouse.model.InvoiceStatus;
import edu.university.warehouse.model.InvoiceType;
import edu.university.warehouse.model.Product;
import edu.university.warehouse.model.ProductCategory;
import edu.university.warehouse.model.Supplier;
import edu.university.warehouse.repository.InvoiceRepository;
import edu.university.warehouse.repository.ProductRepository;
import edu.university.warehouse.repository.SupplierRepository;
import edu.university.warehouse.util.IdGenerator;

import java.math.BigDecimal;
import java.util.List;

public class InventoryService {
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final InvoiceRepository invoiceRepository;

    public InventoryService(ProductRepository productRepository,
                            SupplierRepository supplierRepository,
                            InvoiceRepository invoiceRepository) {
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public Product registerProduct(Product product) {
        if (productRepository.findById(product.getSku()).isPresent()) {
            throw new DomainException("Product already exists with SKU: " + product.getSku());
        }
        return productRepository.save(product);
    }

    public Product updateProduct(String sku, String name, ProductCategory category, BigDecimal unitPrice, int reorderLevel) {
        Product product = findProduct(sku);
        product.updateDetails(name, category, unitPrice, reorderLevel);
        return productRepository.save(product);
    }

    public boolean deleteProduct(String sku) {
        return productRepository.deleteById(normalize(sku));
    }

    public boolean deleteSupplier(String supplierId) {
        return supplierRepository.deleteById(normalize(supplierId));
    }

    public Supplier unassignSupplierSku(String supplierId, String sku) {
        Supplier supplier = supplierRepository.findById(normalize(supplierId))
                .orElseThrow(() -> new DomainException("Supplier not found: " + supplierId));
        supplier.unassignSku(sku);
        return supplierRepository.save(supplier);
    }

    public boolean deleteInvoice(String invoiceId) {
        Invoice invoice = invoiceRepository.findById(normalize(invoiceId))
                .orElseThrow(() -> new DomainException("Invoice not found: " + invoiceId));
        if (invoice.getStatus() == InvoiceStatus.APPROVED) {
            rollbackStockMovement(invoice);
        }
        return invoiceRepository.deleteById(normalize(invoiceId));
    }

    public Supplier registerSupplier(Supplier supplier) {
        if (supplierRepository.findById(supplier.getSupplierId()).isPresent()) {
            throw new DomainException("Supplier already exists with ID: " + supplier.getSupplierId());
        }
        return supplierRepository.save(supplier);
    }

    public Invoice createInvoice(InvoiceType type, String supplierId) {
        if (type == InvoiceType.INCOMING) {
            if (supplierId == null || supplierId.isBlank()) {
                throw new DomainException("Incoming invoice requires supplier ID");
            }
            supplierRepository.findById(supplierId.toUpperCase())
                    .orElseThrow(() -> new DomainException("Supplier not found: " + supplierId));
        }

        Invoice invoice = new Invoice(IdGenerator.newInvoiceId(), type, supplierId);
        return invoiceRepository.save(invoice);
    }

    public Invoice addInvoiceItem(String invoiceId, String sku, int quantity, BigDecimal unitPrice) {
        Invoice invoice = findInvoice(invoiceId);
        Product product = findProduct(sku);

        if (invoice.getType() == InvoiceType.INCOMING && !supplierCanSupply(invoice.getSupplierId(), product.getSku())) {
            throw new DomainException("Supplier " + invoice.getSupplierId() + " is not assigned to SKU: " + product.getSku());
        }

        invoice.addItem(new InvoiceItem(product.getSku(), quantity, unitPrice));
        return invoiceRepository.save(invoice);
    }

    public Invoice approveInvoice(String invoiceId) {
        Invoice invoice = findInvoice(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new DomainException("Only draft invoices can be approved");
        }

        applyStockMovement(invoice);
        invoice.approve();
        return invoiceRepository.save(invoice);
    }

    public List<Product> getProductsBelowReorderLevel() {
        return productRepository.findAll().stream()
                .filter(Product::needsReorder)
                .toList();
    }

    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalizedQuery = query.trim().toLowerCase();
        return productRepository.findAll().stream()
                .filter(product -> product.getName().toLowerCase().contains(normalizedQuery)
                        || product.getSku().toLowerCase().contains(normalizedQuery))
                .toList();
    }

    public List<Product> searchProductsByCategory(ProductCategory category) {
        if (category == null) {
            return List.of();
        }

        return productRepository.findAll().stream()
                .filter(product -> product.getCategory() == category)
                .toList();
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Product getProductBySku(String sku) {
        return findProduct(sku);
    }

    public void restoreState(List<Product> products, List<Supplier> suppliers, List<Invoice> invoices) {
        productRepository.clear();
        supplierRepository.clear();
        invoiceRepository.clear();

        for (Product product : products) {
            productRepository.save(product);
        }
        for (Supplier supplier : suppliers) {
            supplierRepository.save(supplier);
        }
        for (Invoice invoice : invoices) {
            invoiceRepository.save(invoice);
        }
    }

    private Product findProduct(String sku) {
        return productRepository.findById(normalize(sku))
                .orElseThrow(() -> new DomainException("Product not found: " + sku));
    }

    private Invoice findInvoice(String invoiceId) {
        return invoiceRepository.findById(normalize(invoiceId))
                .orElseThrow(() -> new DomainException("Invoice not found: " + invoiceId));
    }

    private boolean supplierCanSupply(String supplierId, String sku) {
        Supplier supplier = supplierRepository.findById(normalize(supplierId))
                .orElseThrow(() -> new DomainException("Supplier not found: " + supplierId));
        return supplier.canSupply(sku);
    }

    private void applyStockMovement(Invoice invoice) {
        for (InvoiceItem item : invoice.getItems()) {
            Product product = findProduct(item.getSku());
            if (invoice.getType() == InvoiceType.INCOMING) {
                product.increaseStock(item.getQuantity());
            } else {
                product.decreaseStock(item.getQuantity());
            }
            productRepository.save(product);
        }
    }

    private void rollbackStockMovement(Invoice invoice) {
        for (InvoiceItem item : invoice.getItems()) {
            Product product = productRepository.findById(normalize(item.getSku())).orElse(null);
            if (product != null) {
                if (invoice.getType() == InvoiceType.INCOMING) {
                    product.decreaseStock(item.getQuantity());
                } else {
                    product.increaseStock(item.getQuantity());
                }
                productRepository.save(product);
            }
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
