package com.matpatielectricals.esinventoryanalytics.entities;

import java.time.LocalDate;
import jakarta.persistence.*; // Imports everything

@Entity
@Table(name = "product_orders")
public class ProductOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private int quantity;
    private double price; 
    private String paymentType; 
    private String status;      
    private String transactionId; 
    private LocalDate orderDate;
    private String deliveryStatus = "Pending";

    // ✅ FIXED: Changed String to Employee
    @ManyToOne
    @JoinColumn(name = "delivery_boy_email") 
    private Employee deliveryBoy;

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    // ✅ FIXED Getters/Setters for Employee
    public Employee getDeliveryBoy() { return deliveryBoy; }
    public void setDeliveryBoy(Employee deliveryBoy) { this.deliveryBoy = deliveryBoy; }
}