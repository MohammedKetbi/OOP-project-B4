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
        if (id <= 0 ) {throw new IllegalArgumentException("University ID must be positive.");}
        if (name == null || name.trim().isEmpty()) {throw new IllegalArgumentException("Full name is required");}
        if (email == null || email.trim().isEmpty()) {throw new IllegalArgumentException("Email is required");}
        if (phone == null || phone.trim().isEmpty()) {throw new IllegalArgumentException("Phone number is required");}
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.status = "Pending";
    }

    // Approve the request
    public void approve() {
        if (!"Pending".equals(status)) {throw new IllegalStateException("This request has already been processed.");}
        status = "Approved";
    }

    // Reject the request
    public void reject() {
        if (!"Pending".equals(status)) {throw new IllegalStateException("This request has already been processed.");}
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

    // STORAGE: pending requests must preserve the chosen password too.
    private String password;

    // Use this constructor for NEW registrations. The old one still compiles.
    public AccessRequest(int id, String name, String email, String phone,
                         String password) {
        this(id, name, email, phone);
        rememberPassword(password);
    }

    public void rememberPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration password is required.");
        }
        this.password = password;
    }

    public String getPasswordForStorage() { return password; }

    // STORAGE: restoration is not a new approval or rejection.
    public static AccessRequest restore(int id, String name, String email,
            String phone, String password, String status) {
        if (!"Pending".equals(status) && !"Approved".equals(status) &&
            !"Rejected".equals(status)) {
            throw new IllegalArgumentException("Invalid saved request status.");
        }
        AccessRequest request = new AccessRequest(id, name, email, phone, password);
        request.status = status;
        return request;
    }

}
