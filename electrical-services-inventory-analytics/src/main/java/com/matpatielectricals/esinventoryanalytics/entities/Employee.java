package com.matpatielectricals.esinventoryanalytics.entities;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "employees")
public class Employee {

	// 1. Email is the Primary Key (@Id)
	// This is used for Login and Identification
	@Id
	@Column(nullable = false, unique = true)
	private String email;

	// 2. Serial Number (Stored in Database)
	// We will calculate this (1, 2, 3...) when saving
	@Column(name = "serial_no")
	private Long serialNo;

	// 3. Password Validation
	@Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$", message = "Password must contain at least one digit, one lowercase, one uppercase, one special character, and be at least 8 characters long.")
	private String password;

	private String firstName;
	private String lastName;
	private String phone;

	// Specific Employee Fields
	private String jobTitle; // e.g., Electrician
	private String experience; // e.g., 2 Years

	// Status: "PENDING" or "ACTIVE"
	private String status;
	private String workStatus = "FREE";
	// Address fields
	private String city;
	private String state;
	private String pin;
	private String address;

	@Column(nullable = false)
	private Integer age;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime registrationDate;

	// --- Getters and Setters ---

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Long getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(Long serialNo) {
		this.serialNo = serialNo;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getJobTitle() {
		return jobTitle;
	}

	public void setJobTitle(String jobTitle) {
		this.jobTitle = jobTitle;
	}

	public String getExperience() {
		return experience;
	}

	public void setExperience(String experience) {
		this.experience = experience;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public LocalDateTime getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(LocalDateTime registrationDate) {
		this.registrationDate = registrationDate;
	}

	public String getWorkStatus() {
		return workStatus;
	}

	public void setWorkStatus(String workStatus) {
		this.workStatus = workStatus;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}
}