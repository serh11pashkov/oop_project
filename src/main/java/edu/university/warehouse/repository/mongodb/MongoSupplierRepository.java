package edu.university.warehouse.repository.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import edu.university.warehouse.exception.DomainException;
import edu.university.warehouse.model.Supplier;
import edu.university.warehouse.repository.SupplierRepository;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MongoSupplierRepository implements SupplierRepository {
    private final MongoCollection<Document> collection;

    public MongoSupplierRepository(MongoDatabase database) {
        this.collection = database.getCollection("suppliers");
    }

    @Override
    public Supplier save(Supplier entity) {
        try {
            Document doc = new Document("_id", entity.getSupplierId())
                    .append("name", entity.getName())
                    .append("email", entity.getEmail())
                    .append("phone", entity.getPhone())
                    .append("suppliedSkus", new ArrayList<>(entity.getSuppliedSkus()));

            ReplaceOptions options = new ReplaceOptions().upsert(true);
            collection.replaceOne(Filters.eq("_id", entity.getSupplierId()), doc, options);
            return entity;
        } catch (Exception e) {
            throw new DomainException("Failed to save supplier to MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Supplier> findById(String id) {
        try {
            Document doc = collection.find(Filters.eq("_id", id)).first();
            if (doc == null) {
                return Optional.empty();
            }
            Supplier supplier = new Supplier(
                    doc.getString("_id"),
                    doc.getString("name"),
                    doc.getString("email"),
                    doc.getString("phone")
            );
            List<String> skus = doc.getList("suppliedSkus", String.class);
            if (skus != null) {
                for (String sku : skus) {
                    supplier.assignSku(sku);
                }
            }
            return Optional.of(supplier);
        } catch (Exception e) {
            throw new DomainException("Failed to query supplier from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Supplier> findAll() {
        try {
            List<Supplier> suppliers = new ArrayList<>();
            for (Document doc : collection.find()) {
                Supplier supplier = new Supplier(
                        doc.getString("_id"),
                        doc.getString("name"),
                        doc.getString("email"),
                        doc.getString("phone")
                );
                List<String> skus = doc.getList("suppliedSkus", String.class);
                if (skus != null) {
                    for (String sku : skus) {
                        supplier.assignSku(sku);
                    }
                }
                suppliers.add(supplier);
            }
            return suppliers;
        } catch (Exception e) {
            throw new DomainException("Failed to query suppliers from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            throw new DomainException("Failed to delete supplier from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            collection.deleteMany(new Document());
        } catch (Exception e) {
            throw new DomainException("Failed to clear suppliers in MongoDB: " + e.getMessage(), e);
        }
    }
}
