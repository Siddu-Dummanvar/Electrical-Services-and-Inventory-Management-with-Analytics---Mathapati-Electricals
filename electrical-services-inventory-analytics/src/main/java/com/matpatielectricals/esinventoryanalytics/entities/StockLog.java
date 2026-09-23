package com.matpatielectricals.esinventoryanalytics.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_logs")
public class StockLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private String action; // "RESTOCK" (Expense) or "SALE" (Revenue)
    private int quantity;  // Amount added or removed
    
    // Financials
    private double unitPrice;    // Cost Price (if RESTOCK) or Selling Price (if SALE)
    private double totalAmount;  // quantity * unitPrice
    private double profit;       // Only for SALE (Selling - Cost)

    private LocalDateTime timestamp;

    // Standard Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public double getProfit() { return profit; }
    public void setProfit(double profit) { this.profit = profit; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}