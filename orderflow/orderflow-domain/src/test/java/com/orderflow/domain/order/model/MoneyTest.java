package com.orderflow.domain.order.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Money value object")
class MoneyTest {

    @Test
    @DisplayName("normalises scale to the currency's fraction digits")
    void normalisesScale() {
        assertThat(Money.of("10", "EUR").amount().scale()).isEqualTo(2);
        assertThat(Money.of("10.1", "EUR")).isEqualTo(Money.of("10.10", "EUR"));
    }

    @Test
    @DisplayName("adds and multiplies within the same currency")
    void arithmetic() {
        assertThat(Money.of("19.99", "EUR").multiply(2)).isEqualTo(Money.of("39.98", "EUR"));
        assertThat(Money.of("5.00", "EUR").add(Money.of("0.50", "EUR"))).isEqualTo(Money.of("5.50", "EUR"));
    }

    @Test
    @DisplayName("refuses to mix currencies")
    void refusesMixedCurrencies() {
        assertThatThrownBy(() -> Money.of("1.00", "EUR").add(Money.of("1.00", "USD")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("uses banker's rounding (HALF_EVEN)")
    void bankersRounding() {
        assertThat(Money.of("2.125", "EUR")).isEqualTo(Money.of("2.12", "EUR")); // ties to even
        assertThat(Money.of("2.135", "EUR")).isEqualTo(Money.of("2.14", "EUR"));
    }
}
