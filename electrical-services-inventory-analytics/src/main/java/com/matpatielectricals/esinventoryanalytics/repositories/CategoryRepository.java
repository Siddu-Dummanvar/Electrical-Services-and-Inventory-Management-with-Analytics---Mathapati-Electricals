package com.matpatielectricals.esinventoryanalytics.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // ✅ This ensures new categories appear first on the Home page
    List<Category> findAllByOrderByIdDesc();
    
    Category findByName(String name);
    List<Category> findByNameContainingIgnoreCase(String keyword);
}
