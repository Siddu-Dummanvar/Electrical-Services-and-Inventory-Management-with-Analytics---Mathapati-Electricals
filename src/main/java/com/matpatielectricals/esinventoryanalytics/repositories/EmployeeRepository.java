package com.matpatielectricals.esinventoryanalytics.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.matpatielectricals.esinventoryanalytics.entities.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    
    // Since Email is the ID, findById(email) works automatically.
    
    // We need this query to calculate the next Serial Number
    // It finds the highest serial number currently in the database
    @Query("SELECT MAX(e.serialNo) FROM Employee e")
    Long findMaxSerialNo();
    long countByStatus(String status);
}