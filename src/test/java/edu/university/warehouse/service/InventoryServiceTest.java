package edu.university.warehouse.service;

import edu.university.warehouse.exception.DomainException;
import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.model.InvoiceType;
import edu.university.warehouse.model.Product;
import edu.university.warehouse.model.ProductCategory;
import edu.university.warehouse.model.Supplier;
import edu.university.warehouse.repository.memory.InMemoryInvoiceRepository;
import edu.university.warehouse.repository.memory.InMemoryProductRepository;
import edu.university.warehouse.repository.memory.InMemorySupplierRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class InventoryServiceTest {
    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(
                new InMemoryProductRepository(),
                new InMemorySupplierRepository(),
                new InMemoryInvoiceRepository()
        );

        Product product = new Product("SKU-1", "Mouse", ProductCategory.OFFICE, new BigDecimal("500.00"), 10, 4);
        service.registerProduct(product);

        Supplier supplier = new Supplier("SUP-1", "Office Partner", "office@example.com", "+380000000000");
        supplier.assignSku("SKU-1");
        service.registerSupplier(supplier);
    }

    @Test
    void shouldIncreaseStockAfterIncomingInvoiceApproval() {
        Invoice invoice = service.createInvoice(InvoiceType.INCOMING, "SUP-1");
        service.addInvoiceItem(invoice.getInvoiceId(), "SKU-1", 5, new BigDecimal("450.00"));
        service.approveInvoice(invoice.getInvoiceId());

        int stock = service.getProductBySku("SKU-1").getQuantityInStock();
        Assertions.assertEquals(15, stock);
    }

    @Test
    void shouldThrowWhenSupplierCannotSupplySku() {
        service.registerProduct(new Product("SKU-2", "Headset", ProductCategory.ELECTRONICS, new BigDecimal("1500.00"), 3, 2));

        Invoice invoice = service.createInvoice(InvoiceType.INCOMING, "SUP-1");

        Assertions.assertThrows(DomainException.class,
                () -> service.addInvoiceItem(invoice.getInvoiceId(), "SKU-2", 1, new BigDecimal("1200.00")));
    }
}
