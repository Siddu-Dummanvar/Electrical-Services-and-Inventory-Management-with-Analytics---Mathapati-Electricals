package com.matpatielectricals.esinventoryanalytics.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // This lets us find all products that match a specific category name
    List<Product> findByCategoryName(String categoryName);
    
    // This lets us find all products that match a specific category ID
    List<Product> findByCategoryId(Long categoryId);
}