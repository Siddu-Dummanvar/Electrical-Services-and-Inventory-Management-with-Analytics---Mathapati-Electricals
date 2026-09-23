package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Employee;
import java.time.LocalDateTime; // ✅ Changed from LocalDate
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    @Query("SELECT MAX(e.serialNo) FROM Employee e")
    Long findMaxSerialNo();
    
    long countByStatus(String status);

    // ✅ FIX: Find by timestamp range (Start of Day -> End of Day)
    List<Employee> findByRegistrationDateBetween(LocalDateTime start, LocalDateTime end);
}