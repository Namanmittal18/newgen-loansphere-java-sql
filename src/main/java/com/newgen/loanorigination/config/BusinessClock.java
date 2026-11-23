package com.newgen.loanorigination.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Component
public class BusinessClock {

    private LocalDate fixedDate = null;

    public BusinessClock(@Value("${loansphere.business.date:}") String configuredDate) {
        if (configuredDate != null && !configuredDate.isBlank()) {
            this.fixedDate = LocalDate.parse(configuredDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    public LocalDate getCurrentDate() {
        return fixedDate != null ? fixedDate : LocalDate.now();
    }

    public LocalDateTime getCurrentDateTime() {
        if (fixedDate != null) {
            return LocalDateTime.of(fixedDate, LocalTime.now());
        }
        return LocalDateTime.now();
    }

    public void setFixedDate(LocalDate date) {
        this.fixedDate = date;
    }

    public void resetToSystemDate() {
        this.fixedDate = null;
    }
}
