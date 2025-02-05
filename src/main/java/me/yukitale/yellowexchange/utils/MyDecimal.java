package me.yukitale.yellowexchange.utils;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MyDecimal {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0.##################");
    private static final DecimalFormat USD_FORMAT = new DecimalFormat("#0.##");

    private final BigDecimal value;
    private final boolean usd;

    public MyDecimal(Double value) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            this.value = BigDecimal.ZERO;
        } else {
            this.value = BigDecimal.valueOf(value);
        }
        this.usd = false;
    }

    public MyDecimal(Double value, boolean usd) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            this.value = BigDecimal.ZERO;
        } else {
            this.value = BigDecimal.valueOf(value);
        }
        this.usd = usd;
    }

    public MyDecimal(BigDecimal value) {
        this.value = value == null ? BigDecimal.ZERO : value;
        this.usd = false;
    }

    public MyDecimal(BigDecimal value, boolean usd) {
        this.value = value == null ? BigDecimal.ZERO : value;
        this.usd = usd;
    }

    public BigDecimal getValue() {
        return value;
    }

    public MyDecimal multiply(Double amount) {
        BigDecimal amt = amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount);
        return new MyDecimal(this.value.multiply(amt));
    }

    public MyDecimal multiply(Double amount, boolean usd) {
        BigDecimal amt = amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount);
        return new MyDecimal(this.value.multiply(amt), usd);
    }

    @Override
    public String toString() {
        if (usd) {
            return USD_FORMAT.format(value).replace(",", ".");
        } else {
            return DECIMAL_FORMAT.format(value).replace(",", ".");
        }
    }

    public String toString(int n) {
        DecimalFormat decimalFormat = new DecimalFormat("#0.###############");
        decimalFormat.setRoundingMode(RoundingMode.DOWN);
        decimalFormat.setMaximumFractionDigits(n);

        BigDecimal scaledValue = value.setScale(n, RoundingMode.DOWN).stripTrailingZeros();

        if (usd) {
            return USD_FORMAT.format(scaledValue).replace(",", ".");
        } else {
            return decimalFormat.format(scaledValue).replace(",", ".");
        }
    }

    public String toStringHigh(int n) {
        DecimalFormat decimalFormat = new DecimalFormat("#0." + "#".repeat(Math.max(0, n)));

        if (usd) {
            return USD_FORMAT.format(value).replace(",", ".");
        } else {
            return decimalFormat.format(value).replace(",", ".");
        }
    }

    public String toStringWithComma() {
        if (usd) {
            return USD_FORMAT.format(value);
        } else {
            return DECIMAL_FORMAT.format(value);
        }
    }

    public String toPrice() {
        int n;
        BigDecimal absValue = value.abs();

        if (absValue.compareTo(new BigDecimal("1E-15")) < 0) {
            n = 18;
        } else if (absValue.compareTo(new BigDecimal("1E-12")) < 0) {
            n = 16;
        } else if (absValue.compareTo(new BigDecimal("1E-9")) < 0) {
            n = 14;
        } else if (absValue.compareTo(new BigDecimal("1E-6")) < 0) {
            n = 12;
        } else if (absValue.compareTo(new BigDecimal("1E-4")) < 0) {
            n = 10;
        } else if (absValue.compareTo(new BigDecimal("1E-3")) < 0) {
            n = 8;
        } else if (absValue.compareTo(BigDecimal.ONE) < 0) {
            n = 6;
        } else if (absValue.compareTo(new BigDecimal("100")) < 0) {
            n = 4;
        } else {
            n = 2;
        }

        return toString(n);
    }
}
