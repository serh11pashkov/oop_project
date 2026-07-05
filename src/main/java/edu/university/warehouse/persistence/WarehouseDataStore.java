package edu.university.warehouse.persistence;

import edu.university.warehouse.service.InventoryService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class WarehouseDataStore {
    private final Path storageFile;

    public WarehouseDataStore(Path storageFile) {
        this.storageFile = storageFile;
    }

    public Optional<WarehouseState> load() {
        if (!Files.exists(storageFile)) {
            return Optional.empty();
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(storageFile))) {
            Object object = inputStream.readObject();
            if (object instanceof WarehouseState state) {
                return Optional.of(state);
            }
            throw new IOException("Unsupported data format");
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Unable to load warehouse data", exception);
        }
    }

    public void save(InventoryService service) {
        try {
            Path parent = storageFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (ObjectOutputStream outputStream = new ObjectOutputStream(Files.newOutputStream(storageFile))) {
                outputStream.writeObject(new WarehouseState(
                        service.getAllProducts(),
                        service.getAllSuppliers(),
                        service.getAllInvoices()));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save warehouse data", exception);
        }
    }

    public void loadInto(InventoryService service, WarehouseState state) {
        service.restoreState(state.getProducts(), state.getSuppliers(), state.getInvoices());
    }
}