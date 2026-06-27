package com.orderflow.domain.order.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A monetary amount in a specific currency.
 * <p>
 * Modelled as an immutable value object to avoid two classic bugs: using
 * {@code double} for money (rounding errors) and silently mixing currencies.
 * All arithmetic enforces same-currency operands and normalises the scale to the
 * currency's fraction digits using {@link RoundingMode#HALF_EVEN} (banker's
 * rounding), the convention expected in financial contexts.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    /** Compact constructor: validates invariants and normalises scale on creation. */
    public Money {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(currency, "currency is required");
        amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
    }

    public static Money of(String amount, String currencyCode) {
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money multiply(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot operate on different currencies: %s vs %s"
                    .formatted(currency.getCurrencyCode(), other.currency.getCurrencyCode()));
        }
    }

    @Override
    public String toString() {
        return "%s %s".formatted(amount.toPlainString(), currency.getCurrencyCode());
    }
}
