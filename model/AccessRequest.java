package model;
import java.io.Serializable;
public class AccessRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    // Store the request details
    private int id;
    private String name;
    private String email;
    private String phone;
    private String status;

    // Create a new request
    public AccessRequest(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = "Pending";
    }

    // Approve the request
    public void approve() {
        status = "Approved";
    }

    // Reject the request
    public void reject() {
        status = "Rejected";
    }

    // Return the request ID
    public int getId() {
        return id;
    }

    // Return the name
    public String getName() {
        return name;
    }

    // Return the email
    public String getEmail() {
        return email;
    }

    // Return the phone number
    public String getPhone() {
        return phone;
    }

    // Return the request status
    public String getStatus() {
        return status;
    }
}
