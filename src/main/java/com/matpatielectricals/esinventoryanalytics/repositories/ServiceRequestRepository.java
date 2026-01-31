package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import com.matpatielectricals.esinventoryanalytics.entities.User;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    
    List<ServiceRequest> findAllByOrderByCreatedOnDesc();
    
    List<ServiceRequest> findByCreatedOnBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // ✅ NEW: Find requests by User (For Customer Analytics)
    List<ServiceRequest> findByUser(User user);
}