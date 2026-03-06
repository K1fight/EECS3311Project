package backend.policy;

import backend.booking.Booking;

public interface PricingStrategy {
    double calculatePrice(Booking booking);
}
