package com.matpatielectricals.esinventoryanalytics.repositories;

import java.util.List; // Import List
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Existing method
    Category findByName(String name);

    // NEW METHOD: Search by name (Like %keyword%)
    List<Category> findByNameContainingIgnoreCase(String keyword);
}
