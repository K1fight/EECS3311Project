package backend.booking;

import backend.user.Client;
import backend.user.Consultant;
import backend.service.ConsultingService;
import java.time.LocalDateTime;
import java.util.UUID;

// State pattern: Booking maintains a State object, delegating state behavior
public class Booking {
    private UUID bookingId;
    private Client client;
    private Consultant consultant;
    private ConsultingService service;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingState currentState;

    public Booking(Client client, Consultant consultant, ConsultingService service, LocalDateTime startTime) {
        this.bookingId = UUID.randomUUID();
        this.client = client;
        this.consultant = consultant;
        this.service = service;
        this.startTime = startTime;
        this.endTime = startTime.plusMinutes(service.getDurationMinutes());
        this.currentState = new RequestedState(); // Initial state
    }

    public void setState(BookingState state) {
        this.currentState = state;
    }

    // Delegate processing to the current state
    public void confirm() {
        currentState.confirm(this);
    }

    public void cancel() {
        currentState.cancel(this);
    }

    public void reject() {
        currentState.reject(this);
    }

    public void markPaid() {
        currentState.markPaid(this);
    }

    public void complete() {
        currentState.complete(this);
    }

    // getters...
    public UUID getBookingId() { return bookingId; }
    public Client getClient() { return client; }
    public Consultant getConsultant() { return consultant; }
    public BookingState getCurrentState() { return currentState; }
    public LocalDateTime getStartTime() { return startTime; }
    public ConsultingService getService() { return service; }
}
