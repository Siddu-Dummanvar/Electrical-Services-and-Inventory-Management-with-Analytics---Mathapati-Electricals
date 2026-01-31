package com.matpatielectricals.esinventoryanalytics.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.ProductOrder;
import com.matpatielectricals.esinventoryanalytics.entities.User;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {
    
    // Find orders for a specific user
    List<ProductOrder> findByUser(User user);
    
    // NEW: Find orders between two dates (for Day/Week/Month/Year graphs)
    List<ProductOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);
    
 // Find all orders for a specific Transaction ID (For Bulk Invoice)
    List<ProductOrder> findByTransactionId(String transactionId); 
}