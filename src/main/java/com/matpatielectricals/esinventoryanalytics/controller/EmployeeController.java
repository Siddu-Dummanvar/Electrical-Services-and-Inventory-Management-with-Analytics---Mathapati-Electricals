package com.matpatielectricals.esinventoryanalytics.controller;

import java.time.LocalDate; // ✅ THIS WAS MISSING
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matpatielectricals.esinventoryanalytics.entities.Employee;
import com.matpatielectricals.esinventoryanalytics.entities.ProductOrder;
import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import com.matpatielectricals.esinventoryanalytics.repositories.EmployeeRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductOrderRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ServiceRequestRepository;
import com.matpatielectricals.esinventoryanalytics.services.EmployeeServices;

import jakarta.servlet.http.HttpSession;

@Controller
public class EmployeeController {

    @Autowired
    private EmployeeServices employeeService;
    
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ProductOrderRepository productOrderRepository;

    // ==========================================
    // 1. REGISTRATION
    // ==========================================
    @GetMapping("/employee-register")
    public String showEmployeeRegisterPage(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee-pages/employee-register";
    }

    @PostMapping("/employee-register")
    public String handleEmployeeRegistration(@ModelAttribute("employee") Employee employee, Model model) {
        try {
            employeeService.registerEmployee(employee);
            model.addAttribute("successMSG", "✅ Registration Successful! Please wait for Admin Approval.");
            model.addAttribute("employee", new Employee());
        } catch (Exception e) {
            model.addAttribute("errorMSG", "❌ Registration Failed! Email might already exist.");
        }
        return "employee-pages/employee-register";
    }

    // ==========================================
    // 2. LOGIN
    // ==========================================
    @GetMapping("/employee-login")
    public String showEmployeeLoginPage() {
        return "employee-pages/employee-login";
    }

    @PostMapping("/employee-login")
    public String handleEmployeeLogin(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        Employee emp = employeeRepository.findById(email).orElse(null);

        if (emp != null && emp.getPassword().equals(password)) {
            if ("ACTIVE".equalsIgnoreCase(emp.getStatus())) {
                session.setAttribute("employeeSession", emp);
                return "redirect:/employee/dashboard";
            } else {
                model.addAttribute("errorMSG", "⚠️ Your account is pending Admin approval.");
                return "employee-pages/employee-login";
            }
        }
        
        model.addAttribute("errorMSG", "❌ Invalid Email or Password.");
        return "employee-pages/employee-login";
    }

    // ==========================================
    // 3. SMART DASHBOARD (Auto-detect Role)
    // ==========================================
    @GetMapping("/employee/dashboard")
    public String showEmployeeDashboard(HttpSession session, Model model) {
        Employee sessionEmp = (Employee) session.getAttribute("employeeSession");
        if (sessionEmp == null) {
            return "redirect:/employee-login";
        }

        // Refresh data from DB
        Employee currentEmp = employeeRepository.findById(sessionEmp.getEmail()).orElse(sessionEmp);
        model.addAttribute("employee", currentEmp);

        String jobTitle = currentEmp.getJobTitle(); 

        // CASE A: Delivery Boy
        if (jobTitle != null && jobTitle.equalsIgnoreCase("Delivery Boy")) {
            List<ProductOrder> myDeliveries = productOrderRepository.findAll().stream()
                .filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(currentEmp.getEmail()))
                .toList();
            model.addAttribute("myDeliveries", myDeliveries);
            model.addAttribute("role", "DELIVERY");
        } 
        // CASE B: Technician/Helper -> Show Service Requests
        else {
            List<ServiceRequest> myTasks = serviceRequestRepository.findAll().stream()
                .filter(req -> req.getAssignedEmployee() != null && req.getAssignedEmployee().getEmail().equals(currentEmp.getEmail()))
                .toList();
            model.addAttribute("myTasks", myTasks);
            model.addAttribute("role", "TECHNICIAN");
        }

        return "employee-pages/employee-dashboard";
    }

    // ==========================================
    // 4. UPDATE STATUS HANDLERS (UPDATED)
    // ==========================================
    
    // For Technicians (Services)
 // For Technicians (Services)
    @PostMapping("/employee/updateServiceStatus")
    public String updateServiceStatus(@RequestParam Long requestId, 
                                      @RequestParam String status, 
                                      HttpSession session) {
        
        ServiceRequest req = serviceRequestRepository.findById(requestId).orElse(null);

        if (req != null) {
            req.setServiceStatus(status);

            // If work is completed, Auto-set Date
            if ("Completed".equals(status)) {
                req.setCompletionDate(LocalDate.now()); // ✅ Auto Date
                
                // Free the employee
                Employee emp = req.getAssignedEmployee();
                if(emp != null) {
                    emp.setWorkStatus("FREE");
                    employeeRepository.save(emp);
                }
            }
            // If work started (Under Progress), mark Employee as BUSY
            else if ("Under Progress".equals(status)) {
                Employee emp = req.getAssignedEmployee();
                if(emp != null) {
                    emp.setWorkStatus("BUSY");
                    employeeRepository.save(emp);
                }
            }
            
            serviceRequestRepository.save(req);
        }
        return "redirect:/employee/dashboard";
    }

    // For Delivery Boys (Products)
    @PostMapping("/employee/updateDeliveryStatus")
    public String updateDeliveryStatus(@RequestParam Long orderId, @RequestParam String status, HttpSession session) {
        ProductOrder order = productOrderRepository.findById(orderId).orElse(null);

        if (order != null) {
            order.setDeliveryStatus(status);
            
            // If Delivered, mark as Paid (Handling Cash Collection)
            if ("Delivered".equalsIgnoreCase(status)) {
                order.setStatus("Paid"); // Updates Admin view to "Paid"
                
                // Free the Delivery Boy
                Employee emp = order.getDeliveryBoy();
                if (emp != null) {
                    emp.setWorkStatus("FREE");
                    employeeRepository.save(emp);
                }
            }
            productOrderRepository.save(order);
        }
        return "redirect:/employee/dashboard";
    }

    // ==========================================
    // 5. LOGOUT
    // ==========================================
    @GetMapping("/employee/logout")
    public String logoutEmployee(HttpSession session) {
        session.invalidate();
        return "redirect:/employee-login";
    }
}