package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** A time grant is saved as fields, not as a display string. */
public final class TimeGrant {
    private final String studentId;
    private final int minutes;
    private final String grantedBy;
    private final LocalDateTime time;
    private final String reason;

    public TimeGrant(String studentId, int minutes, String grantedBy,
                     LocalDateTime time, String reason) {
        if (studentId == null || studentId.trim().isEmpty() || minutes <= 0 ||
            !"admin".equals(grantedBy) || time == null ||
            reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid time grant.");
        }
        this.studentId = studentId;
        this.minutes = minutes;
        this.grantedBy = grantedBy;
        this.time = time;
        this.reason = reason;
    }
    public String getStudentId() { return studentId; }
    public int getMinutes() { return minutes; }
    public String getGrantedBy() { return grantedBy; }
    public LocalDateTime getTime() { return time; }
    public String getReason() { return reason; }
    @Override public String toString() {
        return "Student: " + studentId + ", Minutes: " + minutes +
            ", Granted by: " + grantedBy + ", Time: " +
            time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
            ", Reason: " + reason;
    }
}
