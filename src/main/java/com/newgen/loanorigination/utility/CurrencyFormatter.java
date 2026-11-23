package com.newgen.loanorigination.utility;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class CurrencyFormatter {

    private CurrencyFormatter() {
    }

    /**
     * Formats an amount using Indian numbering format: ₹##,##,###.##
     * Example: 100000.00 -> ₹1,00,000.00
     */
    public static String formatInr(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        long integerPart = scaled.longValue();
        int fractionPart = scaled.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();

        String integerStr = Long.toString(Math.abs(integerPart));
        StringBuilder sb = new StringBuilder();

        int len = integerStr.length();
        if (len <= 3) {
            sb.append(integerStr);
        } else {
            // Last 3 digits
            String lastThree = integerStr.substring(len - 3);
            String remaining = integerStr.substring(0, len - 3);

            // Group remaining in pairs of 2 from right to left
            StringBuilder grouped = new StringBuilder();
            for (int i = remaining.length(); i > 0; i -= 2) {
                int start = Math.max(0, i - 2);
                if (grouped.length() > 0) {
                    grouped.insert(0, ",");
                }
                grouped.insert(0, remaining.substring(start, i));
            }

            sb.append(grouped).append(",").append(lastThree);
        }

        String sign = integerPart < 0 ? "-" : "";
        return String.format("%s₹%s.%02d", sign, sb, fractionPart);
    }

    /**
     * Helper formatting without currency symbol for raw CSV or input fields
     */
    public static String formatIndianNumberOnly(BigDecimal amount) {
        String formatted = formatInr(amount);
        return formatted.replace("₹", "");
    }
}
