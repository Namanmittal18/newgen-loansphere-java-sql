package com.newgen.loanorigination.utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public final class ReferenceGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicLong CUST_SEQUENCE = new AtomicLong(1000);
    private static final AtomicLong APP_SEQUENCE = new AtomicLong(1000);
    private static final AtomicLong DISB_SEQUENCE = new AtomicLong(100000);

    private ReferenceGenerator() {
    }

    public static String generateCustomerCode(LocalDate date) {
        String dateStr = (date != null ? date : LocalDate.now()).format(DATE_FORMATTER);
        return String.format("CUST-%s-%04d", dateStr, CUST_SEQUENCE.incrementAndGet() % 10000);
    }

    public static String generateApplicationNumber(LocalDate date) {
        String dateStr = (date != null ? date : LocalDate.now()).format(DATE_FORMATTER);
        return String.format("APP-%s-%04d", dateStr, APP_SEQUENCE.incrementAndGet() % 10000);
    }

    public static String generateDisbursementReference(LocalDate date) {
        String dateStr = (date != null ? date : LocalDate.now()).format(DATE_FORMATTER);
        return String.format("DISB-%s-%06d", dateStr, DISB_SEQUENCE.incrementAndGet() % 1000000);
    }
}
