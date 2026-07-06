package edu.university.warehouse.persistence;

import io.github.cdimascio.dotenv.Dotenv;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoConnectionManager {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static boolean connected = false;
    private static String errorMessage = "";

    static {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            String uri = dotenv.get("MONGODB_URI");
            if (uri == null || uri.isBlank()) {
                uri = System.getenv("MONGODB_URI");
            }
            if (uri == null || uri.isBlank()) {
                uri = "mongodb://localhost:27017";
            }

            String dbName = dotenv.get("MONGODB_DB");
            if (dbName == null || dbName.isBlank()) {
                dbName = System.getenv("MONGODB_DB");
            }
            if (dbName == null || dbName.isBlank()) {
                dbName = "warehouse_db";
            }

            mongoClient = MongoClients.create(uri);
            database = mongoClient.getDatabase(dbName);
            
            // Ping the database to confirm it's actually reachable
            database.runCommand(new Document("ping", 1));
            connected = true;
        } catch (Exception e) {
            connected = false;
            errorMessage = e.getMessage() != null ? e.getMessage() : e.toString();
        }
    }

    public static MongoClient getClient() {
        return mongoClient;
    }

    public static MongoDatabase getDatabase() {
        return database;
    }

    public static boolean isConnected() {
        return connected;
    }

    public static String getErrorMessage() {
        return errorMessage;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
