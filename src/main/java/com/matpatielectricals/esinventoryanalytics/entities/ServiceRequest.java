package com.matpatielectricals.esinventoryanalytics.entities;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_email")
    private User user;

    private String fullName;
    private String serviceType;
    private LocalDate preferredDate;
    private LocalTime preferredTime;
    private String location;
    private String phoneNumber;
    private String altPhoneNumber;
    private String email;
    @Column(length = 1000)
    private String details;

    // Booking Fee (Online)
    private Double amount; 
    private String paymentStatus; 
    private String transactionId;
    
    // --- NEW FIELDS FOR TECHNICIAN UPDATES ---
    private String serviceStatus = "New Request"; // Default
    private Double amountCollected; // Cash collected by technician
    private LocalDate completionDate; // Date work was finished

    @ManyToOne
    @JoinColumn(name = "employee_email")
    private Employee assignedEmployee;

    @CreationTimestamp
    private LocalDateTime createdOn;

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public LocalDate getPreferredDate() { return preferredDate; }
    public void setPreferredDate(LocalDate preferredDate) { this.preferredDate = preferredDate; }
    public LocalTime getPreferredTime() { return preferredTime; }
    public void setPreferredTime(LocalTime preferredTime) { this.preferredTime = preferredTime; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAltPhoneNumber() { return altPhoneNumber; }
    public void setAltPhoneNumber(String altPhoneNumber) { this.altPhoneNumber = altPhoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }
    public Employee getAssignedEmployee() { return assignedEmployee; }
    public void setAssignedEmployee(Employee assignedEmployee) { this.assignedEmployee = assignedEmployee; }
    public LocalDateTime getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDateTime createdOn) { this.createdOn = createdOn; }
    
    // New Getters/Setters
    public Double getAmountCollected() { return amountCollected; }
    public void setAmountCollected(Double amountCollected) { this.amountCollected = amountCollected; }
    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }
}