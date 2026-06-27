package com.orderflow.domain.shared;

/**
 * Base type for all errors that represent a violation of a business rule.
 * <p>
 * Domain exceptions are part of the domain's vocabulary: they are thrown by the
 * model itself (never by infrastructure) and are translated into transport-level
 * responses (HTTP status, error codes...) at the boundary. Keeping a single base
 * type lets adapters map the whole family with one {@code catch}.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Stable, machine-readable code for this error family. Adapters expose it to
     * clients so they can branch on it without parsing human messages.
     */
    public abstract String code();
}
