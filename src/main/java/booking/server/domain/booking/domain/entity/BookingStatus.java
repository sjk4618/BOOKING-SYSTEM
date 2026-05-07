package booking.server.domain.booking.domain.entity;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_FAILED,
    PAYMENT_UNKNOWN,
    CANCELLED,
    EXPIRED
}
