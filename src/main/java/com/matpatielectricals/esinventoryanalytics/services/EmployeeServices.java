package com.matpatielectricals.esinventoryanalytics.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.matpatielectricals.esinventoryanalytics.entities.Employee;
import com.matpatielectricals.esinventoryanalytics.repositories.EmployeeRepository;

@Service
public class EmployeeServices {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Registers a new employee.
     * Calculates the next Serial Number and sets status to PENDING.
     */
    public void registerEmployee(Employee employee) {
        
        // 1. Logic to Generate Serial Number (1, 2, 3...)
        // We ask the database for the highest current number
        Long maxSerial = employeeRepository.findMaxSerialNo();
        
        if (maxSerial == null) {
            // If database is empty, this is the first employee
            employee.setSerialNo(1L);
        } else {
            // Otherwise, add 1 to the highest number
            employee.setSerialNo(maxSerial + 1);
        }

        // 2. Set Default Status
        employee.setStatus("PENDING"); // Admin must approve later

        // 3. Save to Database
        employeeRepository.save(employee);
    }

    /**
     * Checks login credentials.
     * Returns TRUE only if email/password match AND status is ACTIVE.
     */
    public boolean loginEmployee(String email, String password) {
        // Find by Email (Primary Key)
        Employee emp = employeeRepository.findById(email).orElse(null);

        if (emp != null && emp.getPassword().equals(password)) {
            // Check if Admin has approved them
            if ("ACTIVE".equals(emp.getStatus())) {
                return true; // Login Success
            }
        }
        return false; // Login Failed (Wrong password OR Pending status)
    }
}