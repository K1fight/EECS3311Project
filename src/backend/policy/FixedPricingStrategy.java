package backend.policy;

import backend.booking.Booking;

// Fixed pricing: charge base price only
public class FixedPricingStrategy implements PricingStrategy {
    @Override
    public double calculatePrice(Booking booking) {
        return booking.getService().getBasePrice();
    }
}
