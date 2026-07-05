package edu.university.warehouse.repository.memory;

import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.repository.InvoiceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryInvoiceRepository implements InvoiceRepository {
    private final Map<String, Invoice> storage = new ConcurrentHashMap<>();

    @Override
    public Invoice save(Invoice entity) {
        storage.put(entity.getInvoiceId(), entity);
        return entity;
    }

    @Override
    public Optional<Invoice> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Invoice> findAll() {
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
