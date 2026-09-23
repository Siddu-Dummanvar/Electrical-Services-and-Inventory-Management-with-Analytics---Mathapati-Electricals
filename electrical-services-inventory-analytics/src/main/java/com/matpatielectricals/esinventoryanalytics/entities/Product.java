package com.matpatielectricals.esinventoryanalytics.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@Column(length = 1000)
	private String description;

	private double markedPrice = 0.0;
	private double sellingPrice = 0.0;
	private int stock;
	private String imageName;

	@Column(name = "cost_price")
	private Double costPrice = 0.0;

	// ✅ NEW: Soft Delete Flag
	private boolean deleted = false;

	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdDate;

	@ManyToOne
	@JoinColumn(name = "category_id")
	private Category category;

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

	public Double getCostPrice() {
		return costPrice;
	}

	public void setCostPrice(Double costPrice) {
		this.costPrice = costPrice;
	}

	// ✅ New Getter/Setter for deleted
	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}