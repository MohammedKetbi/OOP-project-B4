
package model;
import java.io.Serializable;
// Manages the GPU time allowance of a student
public class TimeAllowance implements Serializable {
    private static final long serialVersionUID = 1L;
    // Total GPU minutes allocated to the student
    private int allocated;

    // GPU minutes already charged for completed or cancelled jobs
    private int used;

    // GPU minutes reserved for queued or running jobs
    private int reserved;

    // Constructor to initialize the student's allowance
    public TimeAllowance(int allocated) {

        // Prevent negative allocated minutes
        if (allocated < 0) {
            throw new IllegalArgumentException(
                "Allocated minutes cannot be negative"
            );
        }

        this.allocated = allocated;
        this.used = 0;
        this.reserved = 0;
    }

    // Returns the total allocated GPU minutes
    public int getAllocated() {
        return allocated;
    }

    // Returns the GPU minutes already used
    public int getUsed() {
        return used;
    }

    // Returns the GPU minutes currently reserved
    public int getReserved() {
        return reserved;
    }

    // Calculates the remaining available GPU minutes
    public int available() {
        return allocated - used - reserved;
    }

    // Reserves GPU minutes when a job is accepted
    public void reserve(int minutes) {

        // Minutes must be positive
        if (minutes <= 0) {
            throw new IllegalArgumentException(
                "Minutes must be positive"
            );
        }

        // Cannot reserve more than available time
        if (minutes > available()) {
            throw new IllegalArgumentException(
                "Not enough available GPU minutes"
            );
        }

        reserved += minutes;
    }

    // Releases reserved GPU minutes
    public void release(int minutes) {

        // Minutes must be positive
        if (minutes <= 0) {
            throw new IllegalArgumentException(
                "Minutes must be positive"
            );
        }

        // Cannot release more than reserved time
        if (minutes > reserved) {
            throw new IllegalArgumentException(
                "Cannot release more than reserved minutes"
            );
        }

        reserved -= minutes;
    }

    // Charges GPU minutes actually used
    public void charge(int minutes) {

        // Minutes must be positive
        if (minutes <= 0) {
            throw new IllegalArgumentException(
                "Minutes must be positive"
            );
        }

        // Cannot charge minutes reserved for other jobs
        if (minutes > available()) {
            throw new IllegalArgumentException(
                "Not enough unreserved GPU minutes"
            );
        }

        used += minutes;
    }

    // Settles a completed or cancelled job in one operation
    public void settle(int reservedMinutes, int usedMinutes) {

        // Reserved duration must be positive
        if (reservedMinutes <= 0) {
            throw new IllegalArgumentException(
                "Reserved minutes must be positive"
            );
        }

        // Actual runtime cannot be negative or exceed duration
        if (usedMinutes < 0 || usedMinutes > reservedMinutes) {
            throw new IllegalArgumentException(
                "Invalid actual GPU runtime"
            );
        }

        // Cannot release more than currently reserved
        if (reservedMinutes > reserved) {
            throw new IllegalArgumentException(
                "Not enough reserved GPU minutes"
            );
        }

        // Check the final balance before making changes
        if (usedMinutes > allocated - used
                - (reserved - reservedMinutes)) {
            throw new IllegalArgumentException(
                "Insufficient GPU allowance"
            );
        }

        // Release full reservation and charge actual runtime
        reserved -= reservedMinutes;
        used += usedMinutes;
    }

    // Adds extra GPU minutes granted by the administrator
    public void grantTime(int minutes) {

        // Granted minutes must be positive
        if (minutes <= 0) {
            throw new IllegalArgumentException(
                "Granted minutes must be positive"
            );
        }

        allocated = Math.addExact(allocated, minutes);
    }

    // STORAGE: restore totals directly; do not replay reserve()/charge().
    public void restoreTotals(int savedUsed, int savedReserved) {
        if (savedUsed < 0 || savedReserved < 0 ||
            (long) savedUsed + savedReserved > allocated) {
            throw new IllegalArgumentException("Invalid saved allowance totals.");
        }
        used = savedUsed;
        reserved = savedReserved;
    }

}
