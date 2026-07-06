package edu.university.warehouse.repository.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import edu.university.warehouse.exception.DomainException;
import edu.university.warehouse.model.Invoice;
import edu.university.warehouse.model.InvoiceItem;
import edu.university.warehouse.model.InvoiceStatus;
import edu.university.warehouse.model.InvoiceType;
import edu.university.warehouse.repository.InvoiceRepository;
import org.bson.Document;
import org.bson.types.Decimal128;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class MongoInvoiceRepository implements InvoiceRepository {
    private final MongoCollection<Document> collection;

    public MongoInvoiceRepository(MongoDatabase database) {
        this.collection = database.getCollection("invoices");
    }

    @Override
    public Invoice save(Invoice entity) {
        try {
            List<Document> itemDocs = new ArrayList<>();
            for (InvoiceItem item : entity.getItems()) {
                Document itemDoc = new Document("sku", item.getSku())
                        .append("quantity", item.getQuantity())
                        .append("unitPrice", new Decimal128(item.getUnitPrice()));
                itemDocs.add(itemDoc);
            }

            Date date = Date.from(entity.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());

            Document doc = new Document("_id", entity.getInvoiceId())
                    .append("type", entity.getType().name())
                    .append("createdAt", date)
                    .append("supplierId", entity.getSupplierId())
                    .append("status", entity.getStatus().name())
                    .append("items", itemDocs);

            ReplaceOptions options = new ReplaceOptions().upsert(true);
            collection.replaceOne(Filters.eq("_id", entity.getInvoiceId()), doc, options);
            return entity;
        } catch (Exception e) {
            throw new DomainException("Failed to save invoice to MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Invoice> findById(String id) {
        try {
            Document doc = collection.find(Filters.eq("_id", id)).first();
            if (doc == null) {
                return Optional.empty();
            }

            Date date = doc.getDate("createdAt");
            LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

            List<InvoiceItem> items = new ArrayList<>();
            List<Document> itemDocs = doc.getList("items", Document.class);
            if (itemDocs != null) {
                for (Document itemDoc : itemDocs) {
                    InvoiceItem item = new InvoiceItem(
                            itemDoc.getString("sku"),
                            itemDoc.getInteger("quantity"),
                            itemDoc.get("unitPrice", Decimal128.class).bigDecimalValue()
                    );
                    items.add(item);
                }
            }

            Invoice invoice = new Invoice(
                    doc.getString("_id"),
                    InvoiceType.valueOf(doc.getString("type")),
                    doc.getString("supplierId"),
                    ldt,
                    InvoiceStatus.valueOf(doc.getString("status")),
                    items
            );
            return Optional.of(invoice);
        } catch (Exception e) {
            throw new DomainException("Failed to query invoice from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Invoice> findAll() {
        try {
            List<Invoice> invoices = new ArrayList<>();
            for (Document doc : collection.find()) {
                Date date = doc.getDate("createdAt");
                LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());

                List<InvoiceItem> items = new ArrayList<>();
                List<Document> itemDocs = doc.getList("items", Document.class);
                if (itemDocs != null) {
                    for (Document itemDoc : itemDocs) {
                        InvoiceItem item = new InvoiceItem(
                                itemDoc.getString("sku"),
                                itemDoc.getInteger("quantity"),
                                itemDoc.get("unitPrice", Decimal128.class).bigDecimalValue()
                        );
                        items.add(item);
                    }
                }

                Invoice invoice = new Invoice(
                        doc.getString("_id"),
                        InvoiceType.valueOf(doc.getString("type")),
                        doc.getString("supplierId"),
                        ldt,
                        InvoiceStatus.valueOf(doc.getString("status")),
                        items
                );
                invoices.add(invoice);
            }
            return invoices;
        } catch (Exception e) {
            throw new DomainException("Failed to query invoices from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(String id) {
        try {
            return collection.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            throw new DomainException("Failed to delete invoice from MongoDB: " + e.getMessage(), e);
        }
    }

    @Override
    public void clear() {
        try {
            collection.deleteMany(new Document());
        } catch (Exception e) {
            throw new DomainException("Failed to clear invoices in MongoDB: " + e.getMessage(), e);
        }
    }
}
