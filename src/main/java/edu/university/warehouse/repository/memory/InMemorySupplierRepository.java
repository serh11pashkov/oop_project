package edu.university.warehouse.repository.memory;

import edu.university.warehouse.model.Supplier;
import edu.university.warehouse.repository.SupplierRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySupplierRepository implements SupplierRepository {
    private final Map<String, Supplier> storage = new ConcurrentHashMap<>();

    @Override
    public Supplier save(Supplier entity) {
        storage.put(entity.getSupplierId(), entity);
        return entity;
    }

    @Override
    public Optional<Supplier> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Supplier> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public boolean deleteById(String id) {
        return storage.remove(id) != null;
    }

    @Override
    public void clear() {
        storage.clear();
    }
}
