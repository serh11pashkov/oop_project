package edu.university.warehouse.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static String newInvoiceId() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
