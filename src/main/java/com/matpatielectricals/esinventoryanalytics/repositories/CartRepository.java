package com.matpatielectricals.esinventoryanalytics.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.matpatielectricals.esinventoryanalytics.entities.Cart;
import com.matpatielectricals.esinventoryanalytics.entities.Product;
import com.matpatielectricals.esinventoryanalytics.entities.User;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    
    // Find all cart items for a specific user
    List<Cart> findByUser(User user);
    
    // Check if a specific product is already in the user's cart
    Cart findByUserAndProduct(User user, Product product);
}