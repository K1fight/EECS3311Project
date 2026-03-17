package backend.core;

import java.time.LocalDateTime;
import java.util.UUID;

public class TimeSlot {
    private UUID slotId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        this.slotId = UUID.randomUUID();
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return String.format("TimeSlot[%s - %s]", startTime, endTime);
    }
}
