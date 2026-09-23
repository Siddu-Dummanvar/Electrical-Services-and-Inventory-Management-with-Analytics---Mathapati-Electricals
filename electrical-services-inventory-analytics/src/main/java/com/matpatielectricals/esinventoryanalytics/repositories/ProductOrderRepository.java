package com.matpatielectricals.esinventoryanalytics.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.ProductOrder;
import com.matpatielectricals.esinventoryanalytics.entities.User;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {

	List<ProductOrder> findByUser(User user);

	List<ProductOrder> findByOrderDateBetween(LocalDate startDate, LocalDate endDate);

	List<ProductOrder> findByTransactionId(String transactionId);

	long countByDeliveryStatus(String deliveryStatus);

	long countByReturnStatus(String returnStatus);

	// ✅ NEW: Find orders for a single specific day
	List<ProductOrder> findByOrderDate(LocalDate date);

	// Add this inside ProductOrderRepository interface
	List<ProductOrder> findByDeliveryDate(LocalDate date);
}