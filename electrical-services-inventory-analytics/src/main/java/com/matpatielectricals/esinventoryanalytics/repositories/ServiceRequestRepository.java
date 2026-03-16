package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import java.util.List;
import java.time.LocalDate; // ✅ Import Added

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findAllByOrderByCreatedOnDesc();
    long countByServiceStatus(String serviceStatus);
    List<ServiceRequest> findByUser(com.matpatielectricals.esinventoryanalytics.entities.User user);

    // ✅ NEW: Find services completed on a specific date
    List<ServiceRequest> findByCompletionDate(LocalDate date);
}