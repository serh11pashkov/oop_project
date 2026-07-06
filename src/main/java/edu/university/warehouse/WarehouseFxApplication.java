package edu.university.warehouse;

import edu.university.warehouse.exception.DomainException;
import edu.university.warehouse.model.*;
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

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WarehouseFxApplication extends Application {

    private InventoryService service;
    private WarehouseDataStore dataStore;
    private Stage primaryStage;
    private boolean useMongo = false;

    private void saveState() {
        if (!useMongo) {
            dataStore.save(service);
        }
    }

    // Observable Lists for TableViews
    private final ObservableList<Product> observableProducts = FXCollections.observableArrayList();
    private final ObservableList<Supplier> observableSuppliers = FXCollections.observableArrayList();
    private final ObservableList<Invoice> observableInvoices = FXCollections.observableArrayList();
    private final ObservableList<InvoiceItem> observableInvoiceItems = FXCollections.observableArrayList();

    // UI Root Components
    private BorderPane rootLayout;
    private Label sectionTitleLabel;

    // Stat Labels
    private Label totalProductsLabel;
    private Label lowStockLabel;
    private Label totalSuppliersLabel;
    private Label totalInvoicesLabel;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize backend and persistence
        dataStore = new WarehouseDataStore(Path.of("warehouse-data.bin"));
        
        ProductRepository productRepository;
        SupplierRepository supplierRepository;
        InvoiceRepository invoiceRepository;
        
        if (MongoConnectionManager.isConnected()) {
            useMongo = true;
            productRepository = new MongoProductRepository(MongoConnectionManager.getDatabase());
            supplierRepository = new MongoSupplierRepository(MongoConnectionManager.getDatabase());
            invoiceRepository = new MongoInvoiceRepository(MongoConnectionManager.getDatabase());
        } else {
            useMongo = false;
            productRepository = new InMemoryProductRepository();
            supplierRepository = new InMemorySupplierRepository();
            invoiceRepository = new InMemoryInvoiceRepository();
            
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Помилка підключення до БД");
            alert.setHeaderText("Не вдалося підключитися до MongoDB");
            alert.setContentText("Помилка: " + MongoConnectionManager.getErrorMessage() + "\nДодаток працюватиме у локальному In-Memory режимі.");
            alert.showAndWait();
        }

        service = new InventoryService(productRepository, supplierRepository, invoiceRepository);

        // Load pre-existing data or seed demo data
        prepareInitialData();

        // Build UI structure
        rootLayout = new BorderPane();
        rootLayout.getStyleClass().add("root-layout");

        // Create Navigation Sidebar
        VBox sidebar = createSidebar();
        rootLayout.setLeft(sidebar);

        // Create Main Header
        HBox header = createHeader();
        rootLayout.setTop(header);

        // Show Dashboard by default
        showDashboardView();

        // Configure Scene and Stage
        Scene scene = new Scene(rootLayout, 1150, 750);
        try {
            String cssPath = getClass().getResource("/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Warning: Could not load style.css. UI will render without styles.");
        }

        primaryStage.setTitle("Система управління складом - Warehouse Management System");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void prepareInitialData() {
        if (useMongo) {
            if (service.getAllProducts().isEmpty()) {
                seedDemoData();
            }
        } else {
            Optional<WarehouseState> state = dataStore.load();
            state.ifPresent(warehouseState -> dataStore.loadInto(service, warehouseState));

            if (service.getAllProducts().isEmpty()) {
                seedDemoData();
            }
            saveState();
        }
    }

    private void seedDemoData() {
        // 18 Products
        Product laptop = new Product("SKU-001", "Ноутбук ASUS Vivobook", ProductCategory.ELECTRONICS, new BigDecimal("28000.00"), 8, 3);
        Product keyboard = new Product("SKU-002", "Механічна Клавіатура Keychron", ProductCategory.OFFICE, new BigDecimal("4500.00"), 15, 5);
        Product chair = new Product("SKU-003", "Офісне Крісло Ergonomic", ProductCategory.HOUSEHOLD, new BigDecimal("8900.00"), 2, 4);
        Product monitor = new Product("SKU-004", "Монітор Dell 27", ProductCategory.ELECTRONICS, new BigDecimal("12500.00"), 10, 4);
        Product coffee = new Product("SKU-005", "Кава в зернах 1кг", ProductCategory.FOOD, new BigDecimal("650.00"), 4, 10);
        Product paper = new Product("SKU-006", "Папір А4 Double A (5 пачок)", ProductCategory.OFFICE, new BigDecimal("850.00"), 40, 10);
        Product lamp = new Product("SKU-007", "LED Настільна Лампа", ProductCategory.HOUSEHOLD, new BigDecimal("1100.00"), 3, 5);
        Product mouse = new Product("SKU-008", "Бездротова Миша Logitech", ProductCategory.ELECTRONICS, new BigDecimal("2900.00"), 25, 8);
        Product desk = new Product("SKU-009", "Стіл з регулюванням висоти", ProductCategory.HOUSEHOLD, new BigDecimal("16500.00"), 5, 2);
        Product router = new Product("SKU-010", "Wi-Fi Роутер TP-Link AX55", ProductCategory.ELECTRONICS, new BigDecimal("3200.00"), 12, 4);
        Product tea = new Product("SKU-011", "Чай Зелений Листовий 250г", ProductCategory.FOOD, new BigDecimal("240.00"), 50, 15);
        Product notebook = new Product("SKU-012", "Блокнот Moleskine А5", ProductCategory.OFFICE, new BigDecimal("750.00"), 30, 8);
        Product whiteboard = new Product("SKU-013", "Магнітно-маркерна дошка", ProductCategory.OFFICE, new BigDecimal("2100.00"), 0, 3);
        Product organizer = new Product("SKU-014", "Органайзер для кабелів", ProductCategory.HOUSEHOLD, new BigDecimal("450.00"), 60, 10);
        Product purifier = new Product("SKU-015", "Очищувач повітря Xiaomi", ProductCategory.HOUSEHOLD, new BigDecimal("5400.00"), 1, 3);
        Product hub = new Product("SKU-016", "USB-C Хаб Multiport", ProductCategory.ELECTRONICS, new BigDecimal("1800.00"), 14, 5);
        Product boxes = new Product("SKU-017", "Картонна Коробка Поштова", ProductCategory.OTHER, new BigDecimal("45.00"), 500, 100);
        Product pens = new Product("SKU-018", "Набір гелевих ручок 12 шт", ProductCategory.OFFICE, new BigDecimal("320.00"), 18, 20);

        service.registerProduct(laptop);
        service.registerProduct(keyboard);
        service.registerProduct(chair);
        service.registerProduct(monitor);
        service.registerProduct(coffee);
        service.registerProduct(paper);
        service.registerProduct(lamp);
        service.registerProduct(mouse);
        service.registerProduct(desk);
        service.registerProduct(router);
        service.registerProduct(tea);
        service.registerProduct(notebook);
        service.registerProduct(whiteboard);
        service.registerProduct(organizer);
        service.registerProduct(purifier);
        service.registerProduct(hub);
        service.registerProduct(boxes);
        service.registerProduct(pens);

        // 6 Suppliers
        Supplier supplier1 = new Supplier("SUP-100", "ТОВ ТехноПартс", "sales@technoparts.com.ua", "+380442223344");
        supplier1.assignSku("SKU-001");
        supplier1.assignSku("SKU-002");
        supplier1.assignSku("SKU-004");
        supplier1.assignSku("SKU-008");
        supplier1.assignSku("SKU-010");
        supplier1.assignSku("SKU-016");
        service.registerSupplier(supplier1);

        Supplier supplier2 = new Supplier("SUP-200", "ПП Комфорт-Офіс", "info@comfortoffice.ua", "+380503334455");
        supplier2.assignSku("SKU-003");
        supplier2.assignSku("SKU-006");
        supplier2.assignSku("SKU-007");
        supplier2.assignSku("SKU-009");
        supplier2.assignSku("SKU-014");
        service.registerSupplier(supplier2);

        Supplier supplier3 = new Supplier("SUP-300", "Дистриб'ютор ФудСервіс", "orders@foodservice.com", "+380674445566");
        supplier3.assignSku("SKU-005");
        supplier3.assignSku("SKU-011");
        service.registerSupplier(supplier3);

        Supplier supplier4 = new Supplier("SUP-400", "Світ Світла та Енерго", "light@energo.com.ua", "+380931234567");
        supplier4.assignSku("SKU-007");
        supplier4.assignSku("SKU-015");
        service.registerSupplier(supplier4);

        Supplier supplier5 = new Supplier("SUP-500", "ОфісТрейд Україна", "b2b@officetrade.com.ua", "+380445019090");
        supplier5.assignSku("SKU-002");
        supplier5.assignSku("SKU-006");
        supplier5.assignSku("SKU-012");
        supplier5.assignSku("SKU-013");
        supplier5.assignSku("SKU-018");
        service.registerSupplier(supplier5);

        Supplier supplier6 = new Supplier("SUP-600", "Еко-Упаковка та Логістика", "box@ecopack.ua", "+380509998877");
        supplier6.assignSku("SKU-017");
        service.registerSupplier(supplier6);

        // 12 Demo Invoices (Mix of INCOMING & OUTGOING, APPROVED & DRAFT & CANCELLED)
        // 1. Approved Incoming
        Invoice incoming1 = service.createInvoice(InvoiceType.INCOMING, "SUP-100");
        service.addInvoiceItem(incoming1.getInvoiceId(), "SKU-001", 5, new BigDecimal("27000.00"));
        service.addInvoiceItem(incoming1.getInvoiceId(), "SKU-002", 10, new BigDecimal("4200.00"));
        service.approveInvoice(incoming1.getInvoiceId());

        // 2. Approved Incoming
        Invoice incoming2 = service.createInvoice(InvoiceType.INCOMING, "SUP-200");
        service.addInvoiceItem(incoming2.getInvoiceId(), "SKU-003", 2, new BigDecimal("8500.00"));
        service.addInvoiceItem(incoming2.getInvoiceId(), "SKU-006", 30, new BigDecimal("800.00"));
        service.approveInvoice(incoming2.getInvoiceId());

        // 3. Draft Incoming
        Invoice incoming3 = service.createInvoice(InvoiceType.INCOMING, "SUP-300");
        service.addInvoiceItem(incoming3.getInvoiceId(), "SKU-005", 15, new BigDecimal("600.00"));

        // 4. Approved Outgoing
        Invoice outgoing1 = service.createInvoice(InvoiceType.OUTGOING, null);
        service.addInvoiceItem(outgoing1.getInvoiceId(), "SKU-002", 3, new BigDecimal("4900.00"));
        service.addInvoiceItem(outgoing1.getInvoiceId(), "SKU-006", 10, new BigDecimal("950.00"));
        service.approveInvoice(outgoing1.getInvoiceId());

        // 5. Draft Outgoing
        Invoice outgoing2 = service.createInvoice(InvoiceType.OUTGOING, null);
        service.addInvoiceItem(outgoing2.getInvoiceId(), "SKU-004", 1, new BigDecimal("13500.00"));

        // 6. Approved Incoming (New)
        Invoice incoming4 = service.createInvoice(InvoiceType.INCOMING, "SUP-400");
        service.addInvoiceItem(incoming4.getInvoiceId(), "SKU-007", 10, new BigDecimal("1000.00"));
        service.addInvoiceItem(incoming4.getInvoiceId(), "SKU-015", 3, new BigDecimal("5000.00"));
        service.approveInvoice(incoming4.getInvoiceId());

        // 7. Approved Incoming (New)
        Invoice incoming5 = service.createInvoice(InvoiceType.INCOMING, "SUP-500");
        service.addInvoiceItem(incoming5.getInvoiceId(), "SKU-012", 20, new BigDecimal("700.00"));
        service.addInvoiceItem(incoming5.getInvoiceId(), "SKU-018", 15, new BigDecimal("300.00"));
        service.approveInvoice(incoming5.getInvoiceId());

        // 8. Draft Incoming (New)
        Invoice incoming6 = service.createInvoice(InvoiceType.INCOMING, "SUP-600");
        service.addInvoiceItem(incoming6.getInvoiceId(), "SKU-017", 1000, new BigDecimal("40.00"));

        // 9. Approved Outgoing (New)
        Invoice outgoing3 = service.createInvoice(InvoiceType.OUTGOING, null);
        service.addInvoiceItem(outgoing3.getInvoiceId(), "SKU-001", 1, new BigDecimal("30000.00"));
        service.addInvoiceItem(outgoing3.getInvoiceId(), "SKU-008", 5, new BigDecimal("3200.00"));
        service.approveInvoice(outgoing3.getInvoiceId());

        // 10. Approved Outgoing (New)
        Invoice outgoing4 = service.createInvoice(InvoiceType.OUTGOING, null);
        service.addInvoiceItem(outgoing4.getInvoiceId(), "SKU-010", 2, new BigDecimal("3500.00"));
        service.addInvoiceItem(outgoing4.getInvoiceId(), "SKU-016", 4, new BigDecimal("2000.00"));
        service.approveInvoice(outgoing4.getInvoiceId());

        // 11. Cancelled Incoming (New)
        Invoice incoming7 = service.createInvoice(InvoiceType.INCOMING, "SUP-300");
        service.addInvoiceItem(incoming7.getInvoiceId(), "SKU-011", 40, new BigDecimal("220.00"));
        incoming7.cancel();

        // 12. Cancelled Outgoing (New)
        Invoice outgoing5 = service.createInvoice(InvoiceType.OUTGOING, null);
        service.addInvoiceItem(outgoing5.getInvoiceId(), "SKU-005", 2, new BigDecimal("700.00"));
        outgoing5.cancel();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setAlignment(Pos.CENTER_LEFT);

        sectionTitleLabel = new Label("Панель стану");
        sectionTitleLabel.getStyleClass().add("header-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label brandLabel = new Label("Вектор Склад");
        brandLabel.getStyleClass().add("header-brand");

        header.getChildren().addAll(sectionTitleLabel, spacer, brandLabel);
        return header;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 15, 20, 15));
        sidebar.setSpacing(10);
        sidebar.setPrefWidth(220);

        Label navTitle = new Label("МЕНЮ НАВІГАЦІЇ");
        navTitle.getStyleClass().add("sidebar-title");
        VBox.setMargin(navTitle, new Insets(0, 0, 15, 0));

        Button btnDashboard = createSidebarButton("Панель стану");
        Button btnProducts = createSidebarButton("Товари");
        Button btnSuppliers = createSidebarButton("Постачальники");
        Button btnInvoices = createSidebarButton("Накладні");

        // Clear active class from all buttons and add to the clicked one
        List<Button> buttons = List.of(btnDashboard, btnProducts, btnSuppliers, btnInvoices);

        btnDashboard.setOnAction(e -> {
            setActiveButton(btnDashboard, buttons);
            showDashboardView();
        });
        btnProducts.setOnAction(e -> {
            setActiveButton(btnProducts, buttons);
            showProductsView();
        });
        btnSuppliers.setOnAction(e -> {
            setActiveButton(btnSuppliers, buttons);
            showSuppliersView();
        });
        btnInvoices.setOnAction(e -> {
            setActiveButton(btnInvoices, buttons);
            showInvoicesView();
        });

        // Set default active button
        btnDashboard.getStyleClass().add("sidebar-button-active");

        sidebar.getChildren().addAll(navTitle, btnDashboard, btnProducts, btnSuppliers, btnInvoices);
        return sidebar;
    }

    private Button createSidebarButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        return btn;
    }

    private void setActiveButton(Button activeButton, List<Button> allButtons) {
        for (Button btn : allButtons) {
            btn.getStyleClass().remove("sidebar-button-active");
        }
        activeButton.getStyleClass().add("sidebar-button-active");
    }

    private void refreshStatCounters() {
        if (totalProductsLabel != null) {
            totalProductsLabel.setText(String.valueOf(service.getAllProducts().size()));
        }
        if (lowStockLabel != null) {
            lowStockLabel.setText(String.valueOf(service.getProductsBelowReorderLevel().size()));
        }
        if (totalSuppliersLabel != null) {
            totalSuppliersLabel.setText(String.valueOf(service.getAllSuppliers().size()));
        }
        if (totalInvoicesLabel != null) {
            totalInvoicesLabel.setText(String.valueOf(service.getAllInvoices().size()));
        }
    }

    // ==========================================
    // VIEW 1: DASHBOARD VIEW
    // ==========================================
    private void showDashboardView() {
        sectionTitleLabel.setText("Панель стану та аналітика");

        GridPane dashboardGrid = new GridPane();
        dashboardGrid.setHgap(20);
        dashboardGrid.setVgap(20);
        dashboardGrid.setPadding(new Insets(20));

        // Column Constraints (4 equal columns)
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            dashboardGrid.getColumnConstraints().add(cc);
        }

        // Stats boxes
        VBox cardProducts = createStatCard("Усього Товарів", "0", "card-primary");
        totalProductsLabel = (Label) cardProducts.getChildren().get(1);

        VBox cardLowStock = createStatCard("Критичний Рівень", "0", "card-danger");
        lowStockLabel = (Label) cardLowStock.getChildren().get(1);

        VBox cardSuppliers = createStatCard("Постачальники", "0", "card-success");
        totalSuppliersLabel = (Label) cardSuppliers.getChildren().get(1);

        VBox cardInvoices = createStatCard("Накладні", "0", "card-info");
        totalInvoicesLabel = (Label) cardInvoices.getChildren().get(1);

        refreshStatCounters();

        dashboardGrid.add(cardProducts, 0, 0);
        dashboardGrid.add(cardLowStock, 1, 0);
        dashboardGrid.add(cardSuppliers, 2, 0);
        dashboardGrid.add(cardInvoices, 3, 0);

        // Low stock warning list
        VBox alertsBox = new VBox();
        alertsBox.getStyleClass().add("content-card");
        alertsBox.setPadding(new Insets(15));
        alertsBox.setSpacing(10);
        GridPane.setColumnSpan(alertsBox, 4);

        Label alertsTitle = new Label("[Увага] Товари, що потребують дозамовлення (Критичний запас)");
        alertsTitle.getStyleClass().add("card-subtitle");

        TableView<Product> lowStockTable = new TableView<>();
        lowStockTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lowStockTable.setPrefHeight(300);

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        skuCol.setPrefWidth(120);

        TableColumn<Product, String> nameCol = new TableColumn<>("Назва товару");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);

        TableColumn<Product, String> catCol = new TableColumn<>("Категорія");
        catCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory().name()));
        catCol.setPrefWidth(150);

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Поточний запас");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        qtyCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #f87171; -fx-font-weight: bold;");

        TableColumn<Product, Integer> reorderCol = new TableColumn<>("Критичний ліміт");
        reorderCol.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        reorderCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        lowStockTable.getColumns().addAll(skuCol, nameCol, catCol, qtyCol, reorderCol);

        ObservableList<Product> lowStockProducts = FXCollections.observableArrayList(service.getProductsBelowReorderLevel());
        lowStockTable.setItems(lowStockProducts);

        alertsBox.getChildren().addAll(alertsTitle, lowStockTable);
        dashboardGrid.add(alertsBox, 0, 1);

        rootLayout.setCenter(dashboardGrid);
    }

    private VBox createStatCard(String titleText, String valText, String styleClass) {
        VBox card = new VBox();
        card.getStyleClass().addAll("stat-card", styleClass);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setSpacing(5);

        Label title = new Label(titleText);
        title.getStyleClass().add("stat-title");

        Label val = new Label(valText);
        val.getStyleClass().add("stat-value");

        card.getChildren().addAll(title, val);
        return card;
    }

    // ==========================================
    // VIEW 2: PRODUCTS VIEW
    // ==========================================
    private void showProductsView() {
        sectionTitleLabel.setText("Управління товарами");

        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(20));
        contentBox.setSpacing(15);

        // Search & Filter Toolbar
        HBox toolbar = new HBox();
        toolbar.setSpacing(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Пошук за назвою або SKU...");
        searchField.setPrefWidth(250);
        searchField.getStyleClass().add("search-field");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().add("Усі категорії");
        for (ProductCategory category : ProductCategory.values()) {
            categoryCombo.getItems().add(category.name());
        }
        categoryCombo.setValue("Усі категорії");
        categoryCombo.getStyleClass().add("combo-box-filter");

        CheckBox lowStockOnlyCheckbox = new CheckBox("Тільки дефіцит");
        lowStockOnlyCheckbox.getStyleClass().add("custom-checkbox");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("+ Додати Товар");
        btnAdd.getStyleClass().add("btn-primary");

        Button btnEdit = new Button("Редагувати");
        btnEdit.getStyleClass().add("btn-secondary");

        Button btnDelete = new Button("Видалити");
        btnDelete.getStyleClass().add("btn-danger");

        toolbar.getChildren().addAll(searchField, categoryCombo, lowStockOnlyCheckbox, spacer, btnAdd, btnEdit, btnDelete);

        // TableView setup
        TableView<Product> productsTable = new TableView<>();
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(productsTable, Priority.ALWAYS);

        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        skuCol.setPrefWidth(120);

        TableColumn<Product, String> nameCol = new TableColumn<>("Назва товару");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<Product, String> catCol = new TableColumn<>("Категорія");
        catCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCategory().name()));
        catCol.setPrefWidth(120);

        TableColumn<Product, BigDecimal> priceCol = new TableColumn<>("Ціна за од. (₴)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        priceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        priceCol.setPrefWidth(110);

        TableColumn<Product, Integer> qtyCol = new TableColumn<>("Кількість на складі");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantityInStock"));
        qtyCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        qtyCol.setPrefWidth(120);

        TableColumn<Product, Integer> reorderCol = new TableColumn<>("Мін. запас");
        reorderCol.setCellValueFactory(new PropertyValueFactory<>("reorderLevel"));
        reorderCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        reorderCol.setPrefWidth(100);

        TableColumn<Product, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> {
            Product p = cellData.getValue();
            return new SimpleStringProperty(p.needsReorder() ? "[!] ДЕФІЦИТ" : "[ОК] ДОСТАТНЬО");
        });
        statusCol.setPrefWidth(120);

        productsTable.getColumns().addAll(skuCol, nameCol, catCol, priceCol, qtyCol, reorderCol, statusCol);

        // Populate table
        observableProducts.setAll(service.getAllProducts());
        productsTable.setItems(observableProducts);

        // Filter Logic Function
        Runnable filterAction = () -> {
            String query = searchField.getText().trim().toLowerCase();
            String catFilter = categoryCombo.getValue();
            boolean lowStockOnly = lowStockOnlyCheckbox.isSelected();

            List<Product> filtered = service.getAllProducts().stream().filter(p -> {
                boolean matchesSearch = p.getName().toLowerCase().contains(query) || p.getSku().toLowerCase().contains(query);
                boolean matchesCategory = catFilter.equals("Усі категорії") || p.getCategory().name().equals(catFilter);
                boolean matchesLowStock = !lowStockOnly || p.needsReorder();
                return matchesSearch && matchesCategory && matchesLowStock;
            }).collect(Collectors.toList());

            observableProducts.setAll(filtered);
        };

        // Listeners for filter controls
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterAction.run());
        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> filterAction.run());
        lowStockOnlyCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> filterAction.run());

        // Button Actions
        btnAdd.setOnAction(e -> showAddProductDialog(primaryStage, productsTable));
        btnEdit.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditProductDialog(primaryStage, selected, productsTable);
            } else {
                showWarningAlert("Вибір товару", "Будь ласка, виберіть товар зі списку для редагування.");
            }
        });
        btnDelete.setOnAction(e -> {
            Product selected = productsTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Підтвердження видалення");
                confirm.setHeaderText("Ви впевнені, що хочете видалити товар " + selected.getName() + "?");
                confirm.setContentText("SKU: " + selected.getSku());
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        service.deleteProduct(selected.getSku());
                        saveState();
                        observableProducts.setAll(service.getAllProducts());
                        showInfoAlert("Успішно", "Товар було успішно видалено.");
                    } catch (DomainException ex) {
                        showErrorAlert("Помилка видалення", ex.getMessage());
                    }
                }
            } else {
                showWarningAlert("Вибір товару", "Будь ласка, виберіть товар зі списку для видалення.");
            }
        });

        contentBox.getChildren().addAll(toolbar, productsTable);
        rootLayout.setCenter(contentBox);
    }

    private void showAddProductDialog(Stage parentStage, TableView<Product> table) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Додати новий товар");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        TextField skuInput = new TextField();
        skuInput.setPromptText("Наприклад, SKU-105");
        TextField nameInput = new TextField();
        nameInput.setPromptText("Назва товару");

        ComboBox<ProductCategory> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(ProductCategory.values()));
        categoryCombo.setValue(ProductCategory.ELECTRONICS);

        TextField priceInput = new TextField();
        priceInput.setPromptText("0.00");
        TextField qtyInput = new TextField();
        qtyInput.setPromptText("Початковий запас (напр. 10)");
        TextField reorderInput = new TextField();
        reorderInput.setPromptText("Мінімальний рівень (напр. 3)");

        grid.add(new Label("Артикул (SKU):"), 0, 0);
        grid.add(skuInput, 1, 0);
        grid.add(new Label("Назва товару:"), 0, 1);
        grid.add(nameInput, 1, 1);
        grid.add(new Label("Категорія:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Ціна за одиницю (₴):"), 0, 3);
        grid.add(priceInput, 1, 3);
        grid.add(new Label("Початковий запас:"), 0, 4);
        grid.add(qtyInput, 1, 4);
        grid.add(new Label("Критичний ліміт:"), 0, 5);
        grid.add(reorderInput, 1, 5);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 6, 2, 1);

        Button btnSave = new Button("Зберегти");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 7);

        btnSave.setOnAction(e -> {
            try {
                String sku = skuInput.getText();
                String name = nameInput.getText();
                ProductCategory category = categoryCombo.getValue();
                BigDecimal price = new BigDecimal(priceInput.getText());
                int qty = Integer.parseInt(qtyInput.getText());
                int reorder = Integer.parseInt(reorderInput.getText());

                Product p = new Product(sku, name, category, price, qty, reorder);
                service.registerProduct(p);
                saveState();

                observableProducts.setAll(service.getAllProducts());
                dialog.close();
                showInfoAlert("Успіх", "Товар зареєстровано успішно!");
            } catch (NumberFormatException ex) {
                errorLabel.setText("Ціна та кількість мають бути числовими значеннями!");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            } catch (Exception ex) {
                errorLabel.setText("Невірний формат введення даних.");
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 390);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showEditProductDialog(Stage parentStage, Product product, TableView<Product> table) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Редагувати товар");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        Label skuValLabel = new Label(product.getSku());
        skuValLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        TextField nameInput = new TextField(product.getName());
        ComboBox<ProductCategory> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(ProductCategory.values()));
        categoryCombo.setValue(product.getCategory());

        TextField priceInput = new TextField(product.getUnitPrice().toString());
        TextField qtyInput = new TextField(String.valueOf(product.getQuantityInStock()));
        TextField reorderInput = new TextField(String.valueOf(product.getReorderLevel()));

        grid.add(new Label("Артикул (SKU) [Нередагований]:"), 0, 0);
        grid.add(skuValLabel, 1, 0);
        grid.add(new Label("Назва товару:"), 0, 1);
        grid.add(nameInput, 1, 1);
        grid.add(new Label("Категорія:"), 0, 2);
        grid.add(categoryCombo, 1, 2);
        grid.add(new Label("Ціна за одиницю (₴):"), 0, 3);
        grid.add(priceInput, 1, 3);
        grid.add(new Label("Поточний запас (Кількість):"), 0, 4);
        grid.add(qtyInput, 1, 4);
        grid.add(new Label("Критичний ліміт:"), 0, 5);
        grid.add(reorderInput, 1, 5);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 6, 2, 1);

        Button btnSave = new Button("Оновити");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 7);

        btnSave.setOnAction(e -> {
            try {
                String name = nameInput.getText();
                ProductCategory category = categoryCombo.getValue();
                BigDecimal price = new BigDecimal(priceInput.getText());
                int qty = Integer.parseInt(qtyInput.getText());
                int reorder = Integer.parseInt(reorderInput.getText());

                service.updateProduct(product.getSku(), name, category, price, qty, reorder);
                saveState();

                observableProducts.setAll(service.getAllProducts());
                dialog.close();
                showInfoAlert("Успіх", "Товар успішно оновлено!");
            } catch (NumberFormatException ex) {
                errorLabel.setText("Ціна, кількість та ліміт мають бути числовими значеннями!");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            } catch (Exception ex) {
                errorLabel.setText("Невірний формат введення даних.");
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 400);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==========================================
    // VIEW 3: SUPPLIERS VIEW
    // ==========================================
    private void showSuppliersView() {
        sectionTitleLabel.setText("Управління постачальниками");

        VBox contentBox = new VBox();
        contentBox.setPadding(new Insets(20));
        contentBox.setSpacing(15);

        HBox toolbar = new HBox();
        toolbar.setSpacing(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("Список зареєстрованих постачальників");
        searchLabel.getStyleClass().add("card-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAdd = new Button("Додати постачальника");
        btnAdd.getStyleClass().add("btn-primary");

        Button btnUpdateContact = new Button("Оновити контакти");
        btnUpdateContact.getStyleClass().add("btn-secondary");

        Button btnAssignSku = new Button("Закріпити SKU");
        btnAssignSku.getStyleClass().add("btn-info");

        Button btnUnassignSku = new Button("Відкріпити SKU");
        btnUnassignSku.getStyleClass().add("btn-secondary");

        Button btnDeleteSupplier = new Button("Видалити");
        btnDeleteSupplier.getStyleClass().add("btn-danger");

        toolbar.getChildren().addAll(searchLabel, spacer, btnAdd, btnUpdateContact, btnAssignSku, btnUnassignSku, btnDeleteSupplier);

        TableView<Supplier> suppliersTable = new TableView<>();
        suppliersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(suppliersTable, Priority.ALWAYS);

        TableColumn<Supplier, String> idCol = new TableColumn<>("ID Постачальника");
        idCol.setCellValueFactory(new PropertyValueFactory<>("supplierId"));
        idCol.setPrefWidth(150);

        TableColumn<Supplier, String> nameCol = new TableColumn<>("Назва організації");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<Supplier, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(180);

        TableColumn<Supplier, String> phoneCol = new TableColumn<>("Телефон");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        phoneCol.setPrefWidth(150);

        TableColumn<Supplier, String> skusCol = new TableColumn<>("Постачає товари (SKU)");
        skusCol.setCellValueFactory(cellData -> {
            Supplier s = cellData.getValue();
            if (s.getSuppliedSkus().isEmpty()) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(String.join(", ", s.getSuppliedSkus()));
        });
        skusCol.setPrefWidth(250);

        suppliersTable.getColumns().addAll(idCol, nameCol, emailCol, phoneCol, skusCol);

        // Populate table
        observableSuppliers.setAll(service.getAllSuppliers());
        suppliersTable.setItems(observableSuppliers);

        // Action Handlers
        btnAdd.setOnAction(e -> showAddSupplierDialog(primaryStage));
        btnUpdateContact.setOnAction(e -> {
            Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showUpdateContactDialog(primaryStage, selected);
            } else {
                showWarningAlert("Вибір постачальника", "Виберіть постачальника зі списку для оновлення контактів.");
            }
        });
        btnAssignSku.setOnAction(e -> {
            Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showAssignSkuDialog(primaryStage, selected);
            } else {
                showWarningAlert("Вибір постачальника", "Виберіть постачальника зі списку, щоб закріпити товар.");
            }
        });
        btnUnassignSku.setOnAction(e -> {
            Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                if (selected.getSuppliedSkus().isEmpty()) {
                    showWarningAlert("Відкріплення SKU", "У цього постачальника немає закріплених товарів.");
                    return;
                }
                showUnassignSkuDialog(primaryStage, selected);
            } else {
                showWarningAlert("Вибір постачальника", "Виберіть постачальника зі списку, щоб відкріпити товар.");
            }
        });
        btnDeleteSupplier.setOnAction(e -> {
            Supplier selected = suppliersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Підтвердження видалення");
                confirm.setHeaderText("Ви впевнені, що хочете видалити постачальника " + selected.getName() + "?");
                confirm.setContentText("ID: " + selected.getSupplierId());
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        service.deleteSupplier(selected.getSupplierId());
                        saveState();
                        observableSuppliers.setAll(service.getAllSuppliers());
                        showInfoAlert("Успішно", "Постачальника було успішно видалено.");
                    } catch (DomainException ex) {
                        showErrorAlert("Помилка видалення", ex.getMessage());
                    }
                }
            } else {
                showWarningAlert("Вибір постачальника", "Виберіть постачальника зі списку для видалення.");
            }
        });

        contentBox.getChildren().addAll(toolbar, suppliersTable);
        rootLayout.setCenter(contentBox);
    }

    private void showAddSupplierDialog(Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Додати постачальника");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        TextField idInput = new TextField();
        idInput.setPromptText("Наприклад, SUP-300");
        TextField nameInput = new TextField();
        nameInput.setPromptText("Назва компанії");
        TextField emailInput = new TextField();
        emailInput.setPromptText("email@example.com");
        TextField phoneInput = new TextField();
        phoneInput.setPromptText("+380...");

        grid.add(new Label("ID Постачальника:"), 0, 0);
        grid.add(idInput, 1, 0);
        grid.add(new Label("Назва компанії:"), 0, 1);
        grid.add(nameInput, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(emailInput, 1, 2);
        grid.add(new Label("Телефон:"), 0, 3);
        grid.add(phoneInput, 1, 3);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 4, 2, 1);

        Button btnSave = new Button("Зберегти");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 5);

        btnSave.setOnAction(e -> {
            try {
                String id = idInput.getText();
                String name = nameInput.getText();
                String email = emailInput.getText();
                String phone = phoneInput.getText();

                Supplier s = new Supplier(id, name, email, phone);
                service.registerSupplier(s);
                saveState();

                observableSuppliers.setAll(service.getAllSuppliers());
                dialog.close();
                showInfoAlert("Успіх", "Постачальника зареєстровано!");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            } catch (Exception ex) {
                errorLabel.setText("Помилка при створенні постачальника.");
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 310);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showUpdateContactDialog(Stage parentStage, Supplier supplier) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Оновити контакти");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        Label nameLabel = new Label(supplier.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        TextField emailInput = new TextField(supplier.getEmail());
        TextField phoneInput = new TextField(supplier.getPhone());

        grid.add(new Label("Постачальник:"), 0, 0);
        grid.add(nameLabel, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailInput, 1, 1);
        grid.add(new Label("Телефон:"), 0, 2);
        grid.add(phoneInput, 1, 2);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 3, 2, 1);

        Button btnSave = new Button("Оновити");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 4);

        btnSave.setOnAction(e -> {
            try {
                supplier.updateContact(emailInput.getText(), phoneInput.getText());
                saveState();

                observableSuppliers.setAll(service.getAllSuppliers());
                dialog.close();
                showInfoAlert("Успіх", "Контакти оновлено!");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 280);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showAssignSkuDialog(Stage parentStage, Supplier supplier) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Закріпити товар за постачальником");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        Label nameLabel = new Label(supplier.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> productSkuCombo = new ComboBox<>();
        for (Product product : service.getAllProducts()) {
            productSkuCombo.getItems().add(product.getSku() + " - " + product.getName());
        }
        if (!productSkuCombo.getItems().isEmpty()) {
            productSkuCombo.getSelectionModel().selectFirst();
        }

        grid.add(new Label("Постачальник:"), 0, 0);
        grid.add(nameLabel, 1, 0);
        grid.add(new Label("Виберіть товар:"), 0, 1);
        grid.add(productSkuCombo, 1, 1);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 2, 2, 1);

        Button btnSave = new Button("Закріпити");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 3);

        btnSave.setOnAction(e -> {
            try {
                String selectedItem = productSkuCombo.getValue();
                if (selectedItem == null) {
                    errorLabel.setText("Немає доступних товарів.");
                    return;
                }
                String sku = selectedItem.split(" - ")[0];
                supplier.assignSku(sku);
                saveState();

                observableSuppliers.setAll(service.getAllSuppliers());
                dialog.close();
                showInfoAlert("Успіх", "Товар " + sku + " закріплено за постачальником!");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 230);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showUnassignSkuDialog(Stage parentStage, Supplier supplier) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Відкріпити товар від постачальника");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        Label nameLabel = new Label(supplier.getName());
        nameLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> productSkuCombo = new ComboBox<>();
        for (String sku : supplier.getSuppliedSkus()) {
            Product product = service.getProductBySku(sku);
            String nameText = product != null ? product.getName() : "";
            productSkuCombo.getItems().add(sku + " - " + nameText);
        }
        if (!productSkuCombo.getItems().isEmpty()) {
            productSkuCombo.getSelectionModel().selectFirst();
        }

        grid.add(new Label("Постачальник:"), 0, 0);
        grid.add(nameLabel, 1, 0);
        grid.add(new Label("Закріплений товар:"), 0, 1);
        grid.add(productSkuCombo, 1, 1);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 2, 2, 1);

        Button btnSave = new Button("Відкріпити");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 3);

        btnSave.setOnAction(e -> {
            try {
                String selectedItem = productSkuCombo.getValue();
                if (selectedItem == null) {
                    errorLabel.setText("Товар не обрано.");
                    return;
                }
                String sku = selectedItem.split(" - ")[0];
                service.unassignSupplierSku(supplier.getSupplierId(), sku);
                saveState();

                observableSuppliers.setAll(service.getAllSuppliers());
                dialog.close();
                showInfoAlert("Успіх", "Товар " + sku + " відкріплено від постачальника!");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 230);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==========================================
    // VIEW 4: INVOICES VIEW
    // ==========================================
    private void showInvoicesView() {
        sectionTitleLabel.setText("Управління накладними");

        SplitPane splitPane = new SplitPane();
        splitPane.getStyleClass().add("invoices-split-pane");
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        // LEFT PANEL: List of Invoices
        VBox leftBox = new VBox();
        leftBox.setPadding(new Insets(15));
        leftBox.setSpacing(10);

        HBox leftToolbar = new HBox();
        leftToolbar.setSpacing(10);
        leftToolbar.setAlignment(Pos.CENTER_LEFT);

        Label leftTitle = new Label("Накладні");
        leftTitle.getStyleClass().add("card-subtitle");

        Region leftSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);

        Button btnCreateInvoice = new Button("+ Створити накладну");
        btnCreateInvoice.getStyleClass().add("btn-primary");
        leftToolbar.getChildren().addAll(leftTitle, leftSpacer, btnCreateInvoice);

        TableView<Invoice> invoicesTable = new TableView<>();
        invoicesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(invoicesTable, Priority.ALWAYS);

        TableColumn<Invoice, String> idCol = new TableColumn<>("Код (ID)");
        idCol.setCellValueFactory(new PropertyValueFactory<>("invoiceId"));
        idCol.setPrefWidth(120);

        TableColumn<Invoice, String> typeCol = new TableColumn<>("Тип");
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().name()));
        typeCol.setPrefWidth(100);

        TableColumn<Invoice, String> dateCol = new TableColumn<>("Дата створення");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dateTimeFormatter)));
        dateCol.setPrefWidth(140);

        TableColumn<Invoice, String> supCol = new TableColumn<>("Постачальник");
        supCol.setCellValueFactory(cellData -> {
            String sid = cellData.getValue().getSupplierId();
            return new SimpleStringProperty(sid == null ? "-" : sid);
        });
        supCol.setPrefWidth(120);

        TableColumn<Invoice, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));
        statusCol.setPrefWidth(110);

        invoicesTable.getColumns().addAll(idCol, typeCol, dateCol, supCol, statusCol);

        observableInvoices.setAll(service.getAllInvoices());
        invoicesTable.setItems(observableInvoices);

        leftBox.getChildren().addAll(leftToolbar, invoicesTable);

        // RIGHT PANEL: Details of Selected Invoice
        VBox rightBox = new VBox();
        rightBox.setPadding(new Insets(15));
        rightBox.setSpacing(15);
        rightBox.getStyleClass().add("details-pane");

        Label detailsTitle = new Label("Деталі накладної (виберіть зі списку)");
        detailsTitle.getStyleClass().add("card-subtitle");

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(10);
        detailsGrid.setVgap(8);
        detailsGrid.setPadding(new Insets(10, 0, 10, 0));
        detailsGrid.setVisible(false); // only show when selected

        Label lblId = new Label();
        Label lblType = new Label();
        Label lblDate = new Label();
        Label lblSupplier = new Label();
        Label lblStatus = new Label();
        Label lblTotal = new Label();
        lblTotal.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #818cf8;");

        detailsGrid.add(new Label("Код:"), 0, 0);
        detailsGrid.add(lblId, 1, 0);
        detailsGrid.add(new Label("Тип:"), 0, 1);
        detailsGrid.add(lblType, 1, 1);
        detailsGrid.add(new Label("Дата:"), 0, 2);
        detailsGrid.add(lblDate, 1, 2);
        detailsGrid.add(new Label("Постачальник:"), 0, 3);
        detailsGrid.add(lblSupplier, 1, 3);
        detailsGrid.add(new Label("Статус:"), 0, 4);
        detailsGrid.add(lblStatus, 1, 4);
        detailsGrid.add(new Label("Загальна сума:"), 0, 5);
        detailsGrid.add(lblTotal, 1, 5);

        // Invoice Items Table inside Details Panel
        TableView<InvoiceItem> itemsTable = new TableView<>();
        itemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        itemsTable.setPrefHeight(200);
        VBox.setVgrow(itemsTable, Priority.ALWAYS);

        TableColumn<InvoiceItem, String> itemSkuCol = new TableColumn<>("Товар (SKU)");
        itemSkuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        itemSkuCol.setPrefWidth(120);

        TableColumn<InvoiceItem, Integer> itemQtyCol = new TableColumn<>("Кількість");
        itemQtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        itemQtyCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        itemQtyCol.setPrefWidth(90);

        TableColumn<InvoiceItem, BigDecimal> itemPriceCol = new TableColumn<>("Ціна за од.");
        itemPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        itemPriceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        itemPriceCol.setPrefWidth(100);

        TableColumn<InvoiceItem, BigDecimal> itemTotalCol = new TableColumn<>("Сума");
        itemTotalCol.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));
        itemTotalCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");
        itemTotalCol.setPrefWidth(120);

        itemsTable.getColumns().addAll(itemSkuCol, itemQtyCol, itemPriceCol, itemTotalCol);
        itemsTable.setItems(observableInvoiceItems);

        // Actions panel
        HBox actionsToolbar = new HBox(10);
        actionsToolbar.setAlignment(Pos.CENTER_RIGHT);
        actionsToolbar.setVisible(false);

        Button btnAddItem = new Button("+ Додати товар");
        btnAddItem.getStyleClass().add("btn-info");

        Button btnApprove = new Button("✓ Провести (Затвердити)");
        btnApprove.getStyleClass().add("btn-success");

        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        Button btnDeleteInvoice = new Button("Видалити");
        btnDeleteInvoice.getStyleClass().add("btn-danger");

        actionsToolbar.getChildren().addAll(btnAddItem, btnApprove, btnCancel, btnDeleteInvoice);

        rightBox.getChildren().addAll(detailsTitle, detailsGrid, itemsTable, actionsToolbar);

        // SplitPane distribution
        splitPane.getItems().addAll(leftBox, rightBox);
        splitPane.setDividerPositions(0.55);

        rootLayout.setCenter(splitPane);

        // Table Selection Logic
        invoicesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedInvoice) -> {
            if (selectedInvoice != null) {
                detailsTitle.setText("Деталі накладної: " + selectedInvoice.getInvoiceId());
                detailsGrid.setVisible(true);
                actionsToolbar.setVisible(true);

                lblId.setText(selectedInvoice.getInvoiceId());
                lblType.setText(selectedInvoice.getType().name());
                lblDate.setText(selectedInvoice.getCreatedAt().format(dateTimeFormatter));
                lblSupplier.setText(selectedInvoice.getSupplierId() == null ? "Немає" : selectedInvoice.getSupplierId());
                lblStatus.setText(selectedInvoice.getStatus().name());
                lblTotal.setText(selectedInvoice.getTotalAmount().toString() + " ₴");

                observableInvoiceItems.setAll(selectedInvoice.getItems());

                // Enable/Disable Actions based on Status (Delete is now always enabled)
                if (selectedInvoice.getStatus() == InvoiceStatus.DRAFT) {
                    btnAddItem.setDisable(false);
                    btnApprove.setDisable(false);
                    btnCancel.setDisable(false);
                    btnDeleteInvoice.setDisable(false);
                } else if (selectedInvoice.getStatus() == InvoiceStatus.CANCELLED) {
                    btnAddItem.setDisable(true);
                    btnApprove.setDisable(true);
                    btnCancel.setDisable(true);
                    btnDeleteInvoice.setDisable(false);
                } else {
                    // APPROVED
                    btnAddItem.setDisable(true);
                    btnApprove.setDisable(true);
                    btnCancel.setDisable(true);
                    btnDeleteInvoice.setDisable(false); // Enable delete for APPROVED too!
                }
            } else {
                detailsTitle.setText("Деталі накладної (виберіть зі списку)");
                detailsGrid.setVisible(false);
                actionsToolbar.setVisible(false);
                observableInvoiceItems.clear();
            }
        });

        // Add Invoice Action
        btnCreateInvoice.setOnAction(e -> showCreateInvoiceDialog(primaryStage, invoicesTable));

        // Add Item to Invoice Action
        btnAddItem.setOnAction(e -> {
            Invoice selectedInvoice = invoicesTable.getSelectionModel().getSelectedItem();
            if (selectedInvoice != null) {
                showAddInvoiceItemDialog(primaryStage, selectedInvoice, invoicesTable, itemsTable);
            }
        });

        // Approve Invoice Action
        btnApprove.setOnAction(e -> {
            Invoice selectedInvoice = invoicesTable.getSelectionModel().getSelectedItem();
            if (selectedInvoice != null) {
                try {
                    service.approveInvoice(selectedInvoice.getInvoiceId());
                    saveState();

                    // Refresh Views
                    Invoice updated = service.getAllInvoices().stream()
                            .filter(i -> i.getInvoiceId().equals(selectedInvoice.getInvoiceId()))
                            .findFirst().orElse(selectedInvoice);

                    int selectedIdx = invoicesTable.getSelectionModel().getSelectedIndex();
                    observableInvoices.setAll(service.getAllInvoices());
                    invoicesTable.getSelectionModel().select(selectedIdx);

                    showInfoAlert("Успішно", "Накладну " + updated.getInvoiceId() + " проведено. Складські запаси оновлено.");
                } catch (DomainException ex) {
                    showErrorAlert("Помилка проведення", ex.getMessage());
                }
            }
        });

        // Cancel Invoice Action
        btnCancel.setOnAction(e -> {
            Invoice selectedInvoice = invoicesTable.getSelectionModel().getSelectedItem();
            if (selectedInvoice != null) {
                try {
                    // Update state inside model via service
                    Invoice inv = service.getAllInvoices().stream()
                            .filter(i -> i.getInvoiceId().equals(selectedInvoice.getInvoiceId()))
                            .findFirst().orElse(null);
                    if (inv != null) {
                        inv.cancel();
                        saveState();
                    }

                    int selectedIdx = invoicesTable.getSelectionModel().getSelectedIndex();
                    observableInvoices.setAll(service.getAllInvoices());
                    invoicesTable.getSelectionModel().select(selectedIdx);

                    showInfoAlert("Успішно", "Накладну скасовано.");
                } catch (DomainException ex) {
                    showErrorAlert("Помилка скасування", ex.getMessage());
                }
            }
        });

        // Delete Invoice Action
        btnDeleteInvoice.setOnAction(e -> {
            Invoice selectedInvoice = invoicesTable.getSelectionModel().getSelectedItem();
            if (selectedInvoice != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Підтвердження видалення");
                confirm.setHeaderText("Ви впевнені, що хочете видалити накладну " + selectedInvoice.getInvoiceId() + "?");
                
                if (selectedInvoice.getStatus() == InvoiceStatus.APPROVED) {
                    confirm.setContentText("УВАГА: Ця накладна вже ПРОВЕДЕНА!\nВидалення призведе до автоматичного відкоту її впливу на кількість товарів на складі.");
                } else {
                    confirm.setContentText("Статус накладної: " + selectedInvoice.getStatus());
                }
                
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    try {
                        service.deleteInvoice(selectedInvoice.getInvoiceId());
                        saveState();
                        observableInvoices.setAll(service.getAllInvoices());
                        invoicesTable.getSelectionModel().clearSelection();
                        showInfoAlert("Успішно", "Накладну вилучено з системи та проведено відкат запасів.");
                    } catch (DomainException ex) {
                        showErrorAlert("Помилка видалення", ex.getMessage());
                    }
                }
            }
        });
    }

    private void showCreateInvoiceDialog(Stage parentStage, TableView<Invoice> table) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Створити накладну");

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        ComboBox<InvoiceType> typeCombo = new ComboBox<>(FXCollections.observableArrayList(InvoiceType.values()));
        typeCombo.setValue(InvoiceType.INCOMING);

        ComboBox<String> supplierCombo = new ComboBox<>();
        for (Supplier supplier : service.getAllSuppliers()) {
            supplierCombo.getItems().add(supplier.getSupplierId() + " - " + supplier.getName());
        }
        if (!supplierCombo.getItems().isEmpty()) {
            supplierCombo.getSelectionModel().selectFirst();
        }

        // Logic to toggle supplier select depending on type
        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == InvoiceType.OUTGOING) {
                supplierCombo.setDisable(true);
            } else {
                supplierCombo.setDisable(false);
            }
        });

        grid.add(new Label("Тип накладної:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Постачальник:"), 0, 1);
        grid.add(supplierCombo, 1, 1);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 2, 2, 1);

        Button btnSave = new Button("Створити");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 3);

        btnSave.setOnAction(e -> {
            try {
                InvoiceType type = typeCombo.getValue();
                String supplierId = null;

                if (type == InvoiceType.INCOMING) {
                    String selectedSup = supplierCombo.getValue();
                    if (selectedSup == null) {
                        errorLabel.setText("Потрібно вказати постачальника для прибуткової накладної.");
                        return;
                    }
                    supplierId = selectedSup.split(" - ")[0];
                }

                Invoice invoice = service.createInvoice(type, supplierId);
                saveState();

                observableInvoices.setAll(service.getAllInvoices());
                table.getSelectionModel().select(invoice);
                dialog.close();
                showInfoAlert("Успіх", "Створено нову чернетку накладної: " + invoice.getInvoiceId());
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 460, 240);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void showAddInvoiceItemDialog(Stage parentStage, Invoice invoice, TableView<Invoice> invoiceTable, TableView<InvoiceItem> itemsTable) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Додати товар до накладної " + invoice.getInvoiceId());

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(15);
        grid.getStyleClass().add("dialog-grid");

        ComboBox<String> skuCombo = new ComboBox<>();
        // For Incoming, filter items that can be supplied by the supplier.
        // For Outgoing, show all items in catalog.
        List<Product> availableProducts = new ArrayList<>();
        if (invoice.getType() == InvoiceType.INCOMING) {
            Supplier supplier = service.getAllSuppliers().stream()
                    .filter(s -> s.getSupplierId().equals(invoice.getSupplierId()))
                    .findFirst().orElse(null);

            if (supplier != null) {
                for (Product p : service.getAllProducts()) {
                    if (supplier.canSupply(p.getSku())) {
                        availableProducts.add(p);
                    }
                }
            }
        } else {
            availableProducts.addAll(service.getAllProducts());
        }

        for (Product p : availableProducts) {
            skuCombo.getItems().add(p.getSku() + " - " + p.getName());
        }

        if (!skuCombo.getItems().isEmpty()) {
            skuCombo.getSelectionModel().selectFirst();
        }

        TextField qtyInput = new TextField("1");
        TextField priceInput = new TextField();

        // Update default unit price from product selection
        skuCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String sku = newVal.split(" - ")[0];
                Product p = service.getProductBySku(sku);
                if (p != null) {
                    priceInput.setText(p.getUnitPrice().toString());
                }
            }
        });

        // Set initial price
        if (skuCombo.getValue() != null) {
            String sku = skuCombo.getValue().split(" - ")[0];
            Product p = service.getProductBySku(sku);
            if (p != null) {
                priceInput.setText(p.getUnitPrice().toString());
            }
        }

        grid.add(new Label("Виберіть товар (SKU):"), 0, 0);
        grid.add(skuCombo, 1, 0);
        grid.add(new Label("Кількість:"), 0, 1);
        grid.add(qtyInput, 1, 1);
        grid.add(new Label("Ціна за одиницю (₴):"), 0, 2);
        grid.add(priceInput, 1, 2);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
        grid.add(errorLabel, 0, 3, 2, 1);

        Button btnSave = new Button("Додати");
        btnSave.getStyleClass().add("btn-primary");
        Button btnCancel = new Button("Скасувати");
        btnCancel.getStyleClass().add("btn-secondary");

        HBox buttonBox = new HBox(10, btnSave, btnCancel);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        grid.add(buttonBox, 1, 4);

        btnSave.setOnAction(e -> {
            try {
                String selected = skuCombo.getValue();
                if (selected == null) {
                    errorLabel.setText("Потрібно вибрати товар.");
                    return;
                }
                String sku = selected.split(" - ")[0];
                int qty = Integer.parseInt(qtyInput.getText());
                BigDecimal price = new BigDecimal(priceInput.getText());

                // Check stock limits if outgoing
                if (invoice.getType() == InvoiceType.OUTGOING) {
                    Product p = service.getProductBySku(sku);
                    if (p.getQuantityInStock() < qty) {
                        errorLabel.setText("Недостатньо запасів на складі (доступно: " + p.getQuantityInStock() + ")");
                        return;
                    }
                }

                service.addInvoiceItem(invoice.getInvoiceId(), sku, qty, price);
                saveState();

                // Refresh details
                int selectedIdx = invoiceTable.getSelectionModel().getSelectedIndex();
                observableInvoices.setAll(service.getAllInvoices());
                invoiceTable.getSelectionModel().select(selectedIdx);

                dialog.close();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Кількість та ціна мають бути числовими значеннями.");
            } catch (DomainException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        btnCancel.setOnAction(e -> dialog.close());

        Scene scene = new Scene(grid, 480, 260);
        try {
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception ignored) {}
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ==========================================
    // UTILITY HELPER ALERTS
    // ==========================================
    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarningAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
