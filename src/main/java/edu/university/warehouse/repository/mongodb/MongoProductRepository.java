package edu.university.warehouse.repository.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import edu.university.warehouse.exception.DomainException;
import edu.university.warehouse.model.Product;
import edu.university.warehouse.model.ProductCategory;
import edu.university.warehouse.repository.ProductRepository;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MongoProductRepository implements ProductRepository {
    private final MongoCollection<Document> collection;

    public MongoProductRepository(MongoDatabase database) {
        this.collection = database.getCollection("products");
    }

    @Override
    public Product save(Product entity) {
        try {
            Document doc = new Document("_id", entity.getSku())
                    .append("name", entity.getName())
                    .append("category", entity.getCategory().name())
                    .append("unitPrice", new Decimal128(entity.getUnitPrice()))
                    .append("quantityInStock", entity.getQuantityInStock())
                    .append("reorderLevel", entity.getReorderLevel());

            ReplaceOptions options = new ReplaceOptions().upsert(true);
            collection.replaceOne(Filters.eq("_id", entity.getSku()), doc, options);
            return entity;
        } catch (Exception e) {
            throw new DomainException("Failed to save product to MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Product> findById(String id) {
        try {
            Document doc = collection.find(Filters.eq("_id", id)).first();
            if (doc == null) {
                return Optional.empty();
            }
            Product product = new Product(
                    doc.getString("_id"),
                    doc.getString("name"),
                    ProductCategory.valueOf(doc.getString("category")),
                    doc.get("unitPrice", Decimal128.class).bigDecimalValue(),
                    doc.getInteger("quantityInStock"),
                    doc.getInteger("reorderLevel")
            );
            return Optional.of(product);
        } catch (Exception e) {
            throw new DomainException("Failed to query product from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Product> findAll() {
        try {
            List<Product> products = new ArrayList<>();
            for (Document doc : collection.find()) {
                Product product = new Product(
                        doc.getString("_id"),
                        doc.getString("name"),
                        ProductCategory.valueOf(doc.getString("category")),
                        doc.get("unitPrice", Decimal128.class).bigDecimalValue(),
                        doc.getInteger("quantityInStock"),
                        doc.getInteger("reorderLevel")
                );
                products.add(product);
            }
            return products;
        } catch (Exception e) {
            throw new DomainException("Failed to query products from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            throw new DomainException("Failed to delete product from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            collection.deleteMany(new Document());
        } catch (Exception e) {
            throw new DomainException("Failed to clear products in MongoDB: " + e.getMessage(), e);
        }
    }
}
