package com.matpatielectricals.esinventoryanalytics.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // This will be your "si no"

    private String name;
    
    @Column(length = 1000)
    private String description;

    // Price with scratch-through (e.g., 150.00)
    private double markedPrice;

    // Actual price (e.g., 120.00)
    private double sellingPrice; // This is the "price" for your table

    // Number of items in stock
    private int stock;
    
    // The name of the image file (e.g., "fan.jpg")
    private String imageName; 

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdDate; // This is your "uploaded time"

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category; // This is your "product category"

    // Constructors
    public Product() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getMarkedPrice() {
        return markedPrice;
    }

    public void setMarkedPrice(double markedPrice) {
        this.markedPrice = markedPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

	public int getDiscountPrice() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Column(name = "cost_price")
    private Double costPrice = 0.0; // Default to 0 to avoid null errors

    public Double getCostPrice() { return costPrice; }
    public void setCostPrice(Double costPrice) { this.costPrice = costPrice; }
}