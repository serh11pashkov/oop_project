package edu.university.warehouse.ui;

import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.model.InvoiceItem;
import edu.university.warehouse.model.InvoiceType;
import edu.university.warehouse.model.Product;
import edu.university.warehouse.model.ProductCategory;
import edu.university.warehouse.model.Supplier;
import edu.university.warehouse.persistence.MongoConnectionManager;
import edu.university.warehouse.persistence.WarehouseDataStore;
import edu.university.warehouse.service.InventoryService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Scanner;

public class WarehouseConsoleMenu {
    private final InventoryService service;
    private final WarehouseDataStore dataStore;

    public WarehouseConsoleMenu(InventoryService service, WarehouseDataStore dataStore) {
        this.service = service;
        this.dataStore = dataStore;
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> printProducts("Усі товари", service.getAllProducts());
                    case "2" -> handleAddProduct(scanner);
                    case "3" -> handleUpdateProduct(scanner);
                    case "4" -> handleDeleteProduct(scanner);
                    case "5" -> handleCreateInvoice(scanner);
                    case "6" -> handleSearchByText(scanner);
                    case "7" -> handleSearchByCategory(scanner);
                    case "8" -> printSuppliers("Усі постачальники", service.getAllSuppliers());
                    case "9" -> printInvoices("Усі накладні", service.getAllInvoices());
                    case "10" -> printProducts("Товари нижче рівня замовлення", service.getProductsBelowReorderLevel());
                    case "11" -> saveData();
                    case "12" -> {
                        saveData();
                        System.out.println("Вихід із програми.");
                        running = false;
                    }
                    default -> System.out.println("Невірний вибір. Спробуйте ще раз.");
                }
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("СИСТЕМА УПРАВЛІННЯ СКЛАДОМ");
        System.out.println("========================================");
        System.out.println("1. Показати всі товари");
        System.out.println("2. Додати товар");
        System.out.println("3. Оновити товар");
        System.out.println("4. Видалити товар");
        System.out.println("5. Створити накладну");
        System.out.println("6. Пошук товарів за назвою або SKU");
        System.out.println("7. Пошук товарів за категорією");
        System.out.println("8. Показати постачальників");
        System.out.println("9. Показати накладні");
        System.out.println("10. Показати товари нижче рівня замовлення");
        System.out.println("11. Зберегти резервну копію");
        System.out.println("12. Вийти");
        System.out.print("Ваш вибір: ");
    }

    private void handleCreateInvoice(Scanner scanner) {
        System.out.println("Створення накладної");
        InvoiceType type = readInvoiceType(scanner);
        String supplierId = null;

        if (type == InvoiceType.INCOMING) {
            printSuppliers("Доступні постачальники", service.getAllSuppliers());
            supplierId = readString(scanner, "Введіть ID постачальника");
        }

        try {
            Invoice invoice = service.createInvoice(type, supplierId);
            System.out.println("Чернетку накладної створено: " + invoice.getInvoiceId());
            System.out.println("Додавайте позиції. Щоб завершити, залиште SKU порожнім.");

            while (true) {
                String sku = readString(scanner, "SKU товару");
                if (sku.isBlank()) {
                    break;
                }

                int quantity = readInt(scanner, "Кількість");
                BigDecimal unitPrice = readBigDecimal(scanner, "Ціна за одиницю");

                try {
                    invoice = service.addInvoiceItem(invoice.getInvoiceId(), sku, quantity, unitPrice);
                    System.out.println("Позицію додано.");
                } catch (Exception exception) {
                    System.out.println("Не вдалося додати позицію: " + exception.getMessage());
                }
            }

            if (invoice.getItems().isEmpty()) {
                System.out.println("Накладна без позицій залишена як чернетка.");
                saveDataSilently();
                return;
            }

            printInvoices("Створена накладна", List.of(invoice));
            String approveChoice = readString(scanner, "Підтвердити накладну? (y/n)");
            if (approveChoice.equalsIgnoreCase("y")) {
                service.approveInvoice(invoice.getInvoiceId());
                System.out.println("Накладну підтверджено.");
            }

            saveDataSilently();
        } catch (Exception exception) {
            System.out.println("Не вдалося створити накладну: " + exception.getMessage());
        }
    }

    private void handleAddProduct(Scanner scanner) {
        System.out.println("Додавання нового товару");
        String sku = readString(scanner, "SKU");
        String name = readString(scanner, "Назва");
        ProductCategory category = readCategory(scanner);
        BigDecimal unitPrice = readBigDecimal(scanner, "Ціна за одиницю");
        int quantity = readInt(scanner, "Початкова кількість");
        int reorderLevel = readInt(scanner, "Рівень для замовлення");

        try {
            Product product = new Product(sku, name, category, unitPrice, quantity, reorderLevel);
            service.registerProduct(product);
            saveDataSilently();
            System.out.println("Товар додано.");
        } catch (Exception exception) {
            System.out.println("Не вдалося додати товар: " + exception.getMessage());
        }
    }

    private void handleUpdateProduct(Scanner scanner) {
        System.out.println("Оновлення товару");
        String sku = readString(scanner, "SKU товару для оновлення");
        try {
            Product existing = service.getProductBySku(sku);
            System.out.println("Поточні дані: " + existing);
            String name = readString(scanner, "Нова назва");
            ProductCategory category = readCategory(scanner);
            BigDecimal unitPrice = readBigDecimal(scanner, "Нова ціна за одиницю");
            int quantity = readInt(scanner, "Нова кількість на складі");
            int reorderLevel = readInt(scanner, "Новий рівень для замовлення");

            service.updateProduct(sku, name, category, unitPrice, quantity, reorderLevel);
            saveDataSilently();
            System.out.println("Товар оновлено.");
        } catch (Exception exception) {
            System.out.println("Не вдалося оновити товар: " + exception.getMessage());
        }
    }

    private void handleDeleteProduct(Scanner scanner) {
        System.out.println("Видалення товару");
        String sku = readString(scanner, "SKU товару для видалення");
        boolean deleted = service.deleteProduct(sku);
        if (deleted) {
            saveDataSilently();
            System.out.println("Товар видалено.");
        } else {
            System.out.println("Товар не знайдено.");
        }
    }

    private void handleSearchByText(Scanner scanner) {
        String query = readString(scanner, "Введіть назву або SKU для пошуку");
        List<Product> result = service.searchProducts(query);
        printProducts("Результати пошуку", result);
    }

    private void handleSearchByCategory(Scanner scanner) {
        ProductCategory category = readCategory(scanner);
        printProducts("Товари у категорії " + category, service.searchProductsByCategory(category));
    }

    private void saveData() {
        if (!MongoConnectionManager.isConnected()) {
            dataStore.save(service);
            System.out.println("Дані збережено.");
        } else {
            System.out.println("Дані автоматично збережено в MongoDB.");
        }
    }

    private void saveDataSilently() {
        if (!MongoConnectionManager.isConnected()) {
            dataStore.save(service);
        }
    }

    private void printProducts(String title, List<Product> products) {
        printTitle(title);
        if (products.isEmpty()) {
            System.out.println("  (немає записів)");
            return;
        }

        for (Product product : products) {
            System.out.println("  - SKU: " + product.getSku());
            System.out.println("    Назва: " + product.getName());
            System.out.println("    Категорія: " + product.getCategory());
            System.out.println("    Ціна: " + money(product.getUnitPrice()) + " грн");
            System.out.println("    Кількість на складі: " + product.getQuantityInStock());
            System.out.println("    Рівень для замовлення: " + product.getReorderLevel());
        }
    }

    private void printSuppliers(String title, List<Supplier> suppliers) {
        printTitle(title);
        if (suppliers.isEmpty()) {
            System.out.println("  (немає записів)");
            return;
        }

        for (Supplier supplier : suppliers) {
            System.out.println("  - ID: " + supplier.getSupplierId());
            System.out.println("    Назва: " + supplier.getName());
            System.out.println("    Email: " + valueOrDash(supplier.getEmail()));
            System.out.println("    Телефон: " + valueOrDash(supplier.getPhone()));
            System.out.println("    Дозволені SKU: " + supplier.getSuppliedSkus());
        }
    }

    private void printInvoices(String title, List<Invoice> invoices) {
        printTitle(title);
        if (invoices.isEmpty()) {
            System.out.println("  (немає записів)");
            return;
        }

        for (int index = 0; index < invoices.size(); index++) {
            Invoice invoice = invoices.get(index);
            if (index > 0) {
                System.out.println();
                System.out.println("  ----------------------------------------");
            }

            System.out.println("  Накладна № " + (index + 1));
            System.out.println("    Номер: " + invoice.getInvoiceId());
            System.out.println("    Тип: " + invoice.getType());
            System.out.println("    Статус: " + invoice.getStatus());
            System.out.println("    Постачальник: " + valueOrDash(invoice.getSupplierId()));
            System.out.println("    Створено: " + invoice.getCreatedAt());
            System.out.println("    Позиції:");
            for (InvoiceItem item : invoice.getItems()) {
                System.out.println("      * SKU: " + item.getSku());
                System.out.println("        Кількість: " + item.getQuantity());
                System.out.println("        Ціна за одиницю: " + money(item.getUnitPrice()) + " грн");
                System.out.println("        Сума по рядку: " + money(item.getLineTotal()) + " грн");
            }
            System.out.println("    Загальна сума: " + money(invoice.getTotalAmount()) + " грн");
        }
    }

    private void printTitle(String title) {
        System.out.println();
        System.out.println("----------------------------------------");
        System.out.println(title);
        System.out.println("----------------------------------------");
    }

    private String readString(Scanner scanner, String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }

    private BigDecimal readBigDecimal(Scanner scanner, String prompt) {
        while (true) {
            try {
                System.out.print(prompt + ": ");
                return new BigDecimal(scanner.nextLine().trim()).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception exception) {
                System.out.println("Введіть коректне десяткове число.");
            }
        }
    }

    private int readInt(Scanner scanner, String prompt) {
        while (true) {
            try {
                System.out.print(prompt + ": ");
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (Exception exception) {
                System.out.println("Введіть коректне ціле число.");
            }
        }
    }

    private ProductCategory readCategory(Scanner scanner) {
        while (true) {
            System.out.println("Доступні категорії:");
            for (ProductCategory category : ProductCategory.values()) {
                System.out.println("- " + category);
            }
            System.out.print("Введіть категорію: ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return ProductCategory.valueOf(input);
            } catch (IllegalArgumentException exception) {
                System.out.println("Невірна категорія. Спробуйте ще раз.");
            }
        }
    }

    private InvoiceType readInvoiceType(Scanner scanner) {
        while (true) {
            System.out.println("Тип накладної:");
            System.out.println("- INCOMING");
            System.out.println("- OUTGOING");
            System.out.print("Введіть тип: ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                return InvoiceType.valueOf(input);
            } catch (IllegalArgumentException exception) {
                System.out.println("Невірний тип. Спробуйте ще раз.");
            }
        }
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
