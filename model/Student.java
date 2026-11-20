

package model;

// Student inherits from the abstract User class
public class Student extends User {

    // Attributes of Student
    private String name;
    private String email;
    private String phone;

    // Each student has their own GPU time allowance
    private TimeAllowance allowance;

    // Constructor to initialize student information
    public Student(String id, String password,
                   String name, String email, String phone) {

        // Calls the constructor of the User class
        super(id, password);
        if (name == null || name.trim().isEmpty()) {throw new IllegalArgumentException("Full name is required.");}
        if (email == null || email.trim().isEmpty()) {throw new IllegalArgumentException("Email is required.");}
        if (phone == null || phone.trim().isEmpty()) {throw new IllegalArgumentException("Phone number is required.");}
        this.name = name;
        this.email = email;
        this.phone = phone;

        // Approved students receive 100 GPU minutes by default
        this.allowance = new TimeAllowance(100);
    }

    // Constructor that allows a different initial GPU allowance
    public Student(String id, String password,
                   String name, String email,
                   String phone, int allocatedMinutes) {

        // Calls the constructor of the User class
        super(id, password);
        if (name == null || name.trim().isEmpty()) {throw new IllegalArgumentException("Full name is required.");}
        if (email == null || email.trim().isEmpty()) {throw new IllegalArgumentException("Email is required.");}
        if (phone == null || phone.trim().isEmpty()) {throw new IllegalArgumentException("Phone number is required.");}
        this.name = name;
        this.email = email;
        this.phone = phone;

        // Initializes the allowance with the specified minutes
        this.allowance = new TimeAllowance(allocatedMinutes);
    }

    // Returns the student's full name
    public String getName() {
        return name;
    }

    // Returns the student's email address
    public String getEmail() {
        return email;
    }

    // Returns the student's university ID
    public String getStudentId() {
        return getUsername();
    }

    // Returns the student's phone number
    public String getPhone() {
        return phone;
    }

    // Returns the student's GPU time allowance
    public TimeAllowance getAllowance() {
        return allowance;
    }}


