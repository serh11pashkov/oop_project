package edu.university.warehouse;

import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.model.InvoiceType;
import edu.university.warehouse.model.Product;
import edu.university.warehouse.model.ProductCategory;
import edu.university.warehouse.model.Supplier;
import edu.university.warehouse.persistence.MongoConnectionManager;
import edu.university.warehouse.persistence.WarehouseDataStore;
import edu.university.warehouse.persistence.WarehouseState;
import edu.university.warehouse.repository.InvoiceRepository;
import edu.university.warehouse.repository.ProductRepository;
import edu.university.warehouse.repository.SupplierRepository;
import edu.university.warehouse.repository.memory.InMemoryInvoiceRepository;
import edu.university.warehouse.repository.memory.InMemoryProductRepository;
import edu.university.warehouse.repository.memory.InMemorySupplierRepository;
import edu.university.warehouse.repository.mongodb.MongoInvoiceRepository;
import edu.university.warehouse.repository.mongodb.MongoProductRepository;
import edu.university.warehouse.repository.mongodb.MongoSupplierRepository;
import edu.university.warehouse.service.InventoryService;
import edu.university.warehouse.ui.WarehouseConsoleMenu;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;

public class WarehouseApplication {
    public static void main(String[] args) {
        WarehouseDataStore dataStore = new WarehouseDataStore(Path.of("warehouse-data.bin"));
        
        ProductRepository productRepository;
        SupplierRepository supplierRepository;
        InvoiceRepository invoiceRepository;
        boolean useMongo = false;

        if (MongoConnectionManager.isConnected()) {
            useMongo = true;
            productRepository = new MongoProductRepository(MongoConnectionManager.getDatabase());
            supplierRepository = new MongoSupplierRepository(MongoConnectionManager.getDatabase());
            invoiceRepository = new MongoInvoiceRepository(MongoConnectionManager.getDatabase());
        } else {
            System.err.println("Warning: Could not connect to MongoDB (" + MongoConnectionManager.getErrorMessage() + "). Falling back to local offline storage.");
            productRepository = new InMemoryProductRepository();
            supplierRepository = new InMemorySupplierRepository();
            invoiceRepository = new InMemoryInvoiceRepository();
        }

        InventoryService service = new InventoryService(productRepository, supplierRepository, invoiceRepository);

        prepareInitialData(service, dataStore, useMongo);
        new WarehouseConsoleMenu(service, dataStore).run();
    }

    private static void prepareInitialData(InventoryService service, WarehouseDataStore dataStore, boolean useMongo) {
        if (useMongo) {
            if (service.getAllProducts().isEmpty()) {
                seedDemoData(service);
            }
        } else {
            Optional<WarehouseState> state = dataStore.load();
            state.ifPresent(warehouseState -> dataStore.loadInto(service, warehouseState));

            if (service.getAllProducts().isEmpty()) {
                seedDemoData(service);
            }

            dataStore.save(service);
        }
    }

    private static void seedDemoData(InventoryService service) {
        Product laptop = new Product("SKU-001", "Laptop", ProductCategory.ELECTRONICS, new BigDecimal("35000.00"), 5, 3);
        Product keyboard = new Product("SKU-002", "Keyboard", ProductCategory.OFFICE, new BigDecimal("900.00"), 15, 6);

        service.registerProduct(laptop);
        service.registerProduct(keyboard);

        Supplier supplier = new Supplier("SUP-100", "Tech Supplies LLC", "hello@tech.example", "+380501112233");
        supplier.assignSku("SKU-001");
        supplier.assignSku("SKU-002");
        service.registerSupplier(supplier);

        Invoice incomingInvoice = service.createInvoice(InvoiceType.INCOMING, "SUP-100");
        service.addInvoiceItem(incomingInvoice.getInvoiceId(), "SKU-001", 4, new BigDecimal("34000.00"));
        service.addInvoiceItem(incomingInvoice.getInvoiceId(), "SKU-002", 10, new BigDecimal("850.00"));
        service.approveInvoice(incomingInvoice.getInvoiceId());

        Invoice outgoingInvoice = service.createInvoice(InvoiceType.OUTGOING, null);
        service.addInvoiceItem(outgoingInvoice.getInvoiceId(), "SKU-002", 8, new BigDecimal("1200.00"));
        service.approveInvoice(outgoingInvoice.getInvoiceId());
    }
}
