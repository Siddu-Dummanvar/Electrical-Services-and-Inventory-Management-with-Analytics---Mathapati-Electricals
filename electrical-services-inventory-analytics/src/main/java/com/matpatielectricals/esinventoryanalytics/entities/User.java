package com.matpatielectricals.esinventoryanalytics.entities;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "customers")
public class User {

    @Id
    @Column(name = "email_id", nullable = false, unique = true, length = 100)
    @Pattern(
        regexp = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$",
        message = "Invalid email format"
    )
    private String emailid;

    @Column(name = "first_name", nullable = false, length = 50)
    @NotBlank(message = "First name is required")
    @Size(min = 3, max = 15, message = "First name must be between 3 and 15 characters")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    @NotBlank(message = "Last name is required")
    @Size(min = 3, max = 15, message = "Last name must be between 3 and 15 characters")
    private String lastName;


    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[@#$%^&*!]).{8,25}$", 
             message = "Password must contain at least one uppercase letter and one special character (@, #, $, %, ^, &, *, !)")
    private String password;
    
    @Column(name = "phone_no", length = 10)
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must contain exactly 10 digits")
    private String phoneNo;

    @Column(name = "city", length = 30)
    @Size(min = 3, max = 10, message = "City name must be between 3 and 10 characters")
    private String city;

    @Column(name = "state", length = 30)
    @Size(min = 3, max = 10, message = "State name must be between 3 and 10 characters")
    private String state;

    @Column(name = "pin", length = 6)
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be exactly 6 digits")
    private String pin;

    @Column(name = "address", length = 255)
    private String address;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime registrationDate;
    
    @Column(name = "role", length = 10)
    private String role;

    // ✅ Getters & Setters
    public String getEmailid() { return emailid; }
    public void setEmailid(String emailid) { this.emailid = emailid; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNo() { return phoneNo; }
    public void setPhoneNo(String phoneNo) { this.phoneNo = phoneNo; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    
    public LocalDateTime getRegistrationDate() {return registrationDate;}

    public void setRegistrationDate(LocalDateTime registrationDate) {this.registrationDate = registrationDate;}
    
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }
    
    
}
