package com.matpatielectricals.esinventoryanalytics.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.matpatielectricals.esinventoryanalytics.entities.Employee;
import com.matpatielectricals.esinventoryanalytics.entities.Product;
import com.matpatielectricals.esinventoryanalytics.entities.ProductOrder;
import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import com.matpatielectricals.esinventoryanalytics.repositories.EmployeeRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductOrderRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductRepository;
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
	@Autowired
	private ProductRepository productRepository;

	// --- REGISTRATION & LOGIN ---
	@GetMapping("/employee-register")
	public String showEmployeeRegisterPage(Model model) {
		model.addAttribute("employee", new Employee());
		return "employee-pages/employee-register";
	}

	@PostMapping("/employee-register")
	public String handleEmployeeRegistration(@ModelAttribute("employee") Employee employee, Model model) {
		
		try {
			employeeService.registerEmployee(employee);
			model.addAttribute("successMSG", "✅ Registration Successful! Wait for Admin Approval.");
			model.addAttribute("employee", new Employee());
		} catch (Exception e) {
			model.addAttribute("errorMSG", "❌ Registration Failed!");
		}
		return "employee-pages/employee-register";
	}

	@GetMapping("/employee-login")
	public String showEmployeeLoginPage() {
		return "employee-pages/employee-login";
	}

	@PostMapping("/employee-login")
	public String handleEmployeeLogin(@RequestParam String email, @RequestParam String password, HttpSession session,
			Model model) {
		Employee emp = employeeRepository.findById(email).orElse(null);
		if (emp != null && emp.getPassword().equals(password)) {
			if ("ACTIVE".equalsIgnoreCase(emp.getStatus())) {
				session.setAttribute("employeeSession", emp);
				return "redirect:/employee/dashboard";
			} else {
				model.addAttribute("errorMSG", "⚠️ Account Pending Approval.");
				return "employee-pages/employee-login";
			}
		}
		model.addAttribute("errorMSG", "❌ Invalid Credentials.");
		return "employee-pages/employee-login";
	}

	// ==========================================
	// 1. DASHBOARD (PENDING WORK)
	// ==========================================
	@GetMapping({ "/employee/dashboard", "/employee/pending-work" })
	public String showEmployeeDashboard(HttpSession session, Model model) {
		Employee sessionEmp = (Employee) session.getAttribute("employeeSession");
		if (sessionEmp == null)
			return "redirect:/employee-login";

		Employee currentEmp = employeeRepository.findById(sessionEmp.getEmail()).orElse(sessionEmp);
		model.addAttribute("employee", currentEmp);

		if ("Delivery Boy".equalsIgnoreCase(currentEmp.getJobTitle())) {
			// Pending Deliveries
			List<ProductOrder> myDeliveries = productOrderRepository.findAll().stream().filter(
					o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(currentEmp.getEmail()))
					.filter(o -> "Pending".equals(o.getDeliveryStatus())
							|| "Out for Delivery".equals(o.getDeliveryStatus()))
					.toList();

			// Pending Returns
			List<ProductOrder> myReturns = productOrderRepository.findAll().stream().filter(
					o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(currentEmp.getEmail()))
					.filter(o -> "Pickup Assigned".equals(o.getReturnStatus())).toList();

			model.addAttribute("myDeliveries", myDeliveries);
			model.addAttribute("myReturns", myReturns);
			model.addAttribute("role", "DELIVERY");
		} else {
			// Pending Services
			List<ServiceRequest> myTasks = serviceRequestRepository.findAll().stream()
					.filter(req -> req.getAssignedEmployee() != null
							&& req.getAssignedEmployee().getEmail().equals(currentEmp.getEmail()))
					.filter(req -> !"Completed".equalsIgnoreCase(req.getServiceStatus())).toList();
			model.addAttribute("myTasks", myTasks);
			model.addAttribute("role", "TECHNICIAN");
		}
		return "employee-pages/employee-dashboard";
	}

	// ==========================================
	// 2. WORK DONE (HISTORY)
	// ==========================================
	@GetMapping("/employee/work-done")
	public String showWorkHistory(HttpSession session, Model model) {
		Employee sessionEmp = (Employee) session.getAttribute("employeeSession");
		if (sessionEmp == null)
			return "redirect:/employee-login";

		Employee currentEmp = employeeRepository.findById(sessionEmp.getEmail()).orElse(sessionEmp);
		model.addAttribute("employee", currentEmp);

		if ("Delivery Boy".equalsIgnoreCase(currentEmp.getJobTitle())) {
			// Completed Deliveries (Status = Delivered) OR Completed Returns (Status =
			// Refunded)
			List<ProductOrder> history = productOrderRepository.findAll().stream().filter(
					o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(currentEmp.getEmail()))
					.filter(o -> "Delivered".equals(o.getDeliveryStatus()) || "Refunded".equals(o.getReturnStatus()))
					.toList();

			model.addAttribute("history", history);
			model.addAttribute("role", "DELIVERY");
		} else {
			// Completed Services
			List<ServiceRequest> history = serviceRequestRepository.findAll().stream()
					.filter(req -> req.getAssignedEmployee() != null
							&& req.getAssignedEmployee().getEmail().equals(currentEmp.getEmail()))
					.filter(req -> "Completed".equalsIgnoreCase(req.getServiceStatus())).toList();
			model.addAttribute("history", history);
			model.addAttribute("role", "TECHNICIAN");
		}
		return "employee-pages/employee-work-history";
	}

	// --- ACTIONS ---

	@PostMapping("/employee/completeReturn")
	public String completeReturn(@RequestParam Long orderId, HttpSession session) {
		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		if (order != null) {
			order.setReturnStatus("Refunded");
			order.setStatus("Returned");
			Product p = order.getProduct();
			p.setStock(p.getStock() + order.getQuantity());
			productRepository.save(p);

			// Free Employee
			Employee emp = order.getDeliveryBoy();
			if (emp != null) {
				emp.setWorkStatus("FREE");
				employeeRepository.save(emp);
			}

			productOrderRepository.save(order);
		}
		return "redirect:/employee/dashboard";
	}

	@PostMapping("/employee/updateDeliveryStatus")
	public String updateDeliveryStatus(@RequestParam Long orderId, @RequestParam String status) {
		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		if (order != null) {
			order.setDeliveryStatus(status);
			if ("Delivered".equalsIgnoreCase(status)) {
				order.setStatus("Paid");
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

	@PostMapping("/employee/updateServiceStatus")
	public String updateServiceStatus(@RequestParam Long requestId, @RequestParam String status) {
		ServiceRequest req = serviceRequestRepository.findById(requestId).orElse(null);
		if (req != null) {
			req.setServiceStatus(status);
			if ("Completed".equals(status)) {
				req.setCompletionDate(LocalDate.now());
				Employee emp = req.getAssignedEmployee();
				if (emp != null) {
					emp.setWorkStatus("FREE");
					employeeRepository.save(emp);
				}
			} else if ("Under Progress".equals(status)) {
				Employee emp = req.getAssignedEmployee();
				if (emp != null) {
					emp.setWorkStatus("BUSY");
					employeeRepository.save(emp);
				}
			}
			serviceRequestRepository.save(req);
		}
		return "redirect:/employee/dashboard";
	}

	
	// ===============================================
    // EMPLOYEE LOGOUT
    // ===============================================
    @GetMapping("/employee/logout")
    public String logoutEmployee(HttpSession session) {
        session.invalidate(); // Destroys the session
        
        // ✅ CHANGE THIS LINE: Redirect to Landing Page instead of Login Page
        return "redirect:/LandingPage"; 
    }
}