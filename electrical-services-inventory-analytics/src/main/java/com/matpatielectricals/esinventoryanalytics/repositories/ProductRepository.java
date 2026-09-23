package com.matpatielectricals.esinventoryanalytics.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	// Find all products that match a specific category name
	List<Product> findByCategoryName(String categoryName);

	// Old method (finds everything)
	List<Product> findByCategoryId(Long categoryId);

	// ✅ NEW: Find products by Category that are NOT deleted
	List<Product> findByCategoryIdAndDeletedFalse(Long categoryId);
}