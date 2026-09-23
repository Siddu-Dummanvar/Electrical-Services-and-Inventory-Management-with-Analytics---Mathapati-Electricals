package com.matpatielectricals.esinventoryanalytics.controller;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap; // ✅ Added
import java.util.List;
import java.util.Map; // ✅ Added

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.matpatielectricals.esinventoryanalytics.entities.Admin;
import com.matpatielectricals.esinventoryanalytics.entities.Category;
import com.matpatielectricals.esinventoryanalytics.entities.Employee;
import com.matpatielectricals.esinventoryanalytics.entities.Feedback;
import com.matpatielectricals.esinventoryanalytics.entities.Product;
import com.matpatielectricals.esinventoryanalytics.entities.ProductOrder;
import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import com.matpatielectricals.esinventoryanalytics.entities.StockLog;
import com.matpatielectricals.esinventoryanalytics.entities.User;
import com.matpatielectricals.esinventoryanalytics.repositories.AdminRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.CartRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.CategoryRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.EmployeeRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.FeedbackRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductOrderRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ServiceRequestRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.StockLogRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private com.matpatielectricals.esinventoryanalytics.services.ReportService reportService;
	@Autowired
	private CartRepository cartRepository;
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private FeedbackRepository feedbackRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private ProductOrderRepository productOrderRepository;

	@Autowired
	private AdminRepository adminRepository;

	@Autowired
	private StockLogRepository stockLogRepository;

	// ===============================================
	// 1. ADMIN LOGIN
	// ===============================================
	@GetMapping("/login")
	public String showAdminLoginPage(Model model) {
		model.addAttribute("adminUser", new User());
		return "admin-login";
	}

	@PostMapping("/login")
	public String handleAdminLogin(@ModelAttribute("adminUser") User formData, HttpSession session, Model model) {

		// CHECK THE NEW ADMIN TABLE
		Admin admin = adminRepository.findByEmail(formData.getEmailid());

		if (admin != null && admin.getPassword().equals(formData.getPassword())) {
			session.setAttribute("adminSession", admin);
			return "redirect:/admin/dashboard";
		}

		model.addAttribute("errorMSG", "❌ Invalid Admin ID or Password!");
		return "admin-login";
	}

	// ===============================================
	// 2. DASHBOARD
	// ===============================================
	@GetMapping("/dashboard")
	public String showAdminDashboard(@RequestParam(value = "filter", defaultValue = "year") String filter,
			HttpSession session, Model model) {

		if (session.getAttribute("adminSession") == null) {
			return "redirect:/admin/login";
		}

		// --- A. KEY METRICS ---
		List<ProductOrder> allOrders = productOrderRepository.findAll();

		// 1. Calculate Product Revenue (Only "Paid" orders)
		double productRevenue = allOrders.stream().filter(o -> "Paid".equalsIgnoreCase(o.getStatus()))
				.mapToDouble(ProductOrder::getPrice).sum();

		// 2. Calculate Service Revenue (All requests with an amount)
		List<ServiceRequest> allServices = serviceRequestRepository.findAll();
		double serviceRevenue = allServices.stream().filter(s -> s.getAmount() != null)
				.mapToDouble(ServiceRequest::getAmount).sum();

		// 3. Total Revenue
		double totalRevenue = productRevenue + serviceRevenue;

		long totalProducts = productRepository.count();
		long totalEmployees = employeeRepository.count();

		long pendingDeliveries = allOrders.stream().filter(o -> !"Delivered".equalsIgnoreCase(o.getDeliveryStatus()))
				.count();

		long lowStockCount = productRepository.findAll().stream().filter(p -> p.getStock() < 5).count();

		long pendingEmployeeRequests = employeeRepository.countByStatus("PENDING");
		model.addAttribute("pendingEmployeeRequests", pendingEmployeeRequests);

		model.addAttribute("totalRevenue", totalRevenue);
		model.addAttribute("totalProducts", totalProducts);
		model.addAttribute("totalEmployees", totalEmployees);
		model.addAttribute("pendingDeliveries", pendingDeliveries);
		model.addAttribute("lowStockCount", lowStockCount);
		model.addAttribute("currentFilter", filter);

		// --- B. CHART DATA LOGIC ---
		List<String> chartLabels = new ArrayList<>();
		List<Double> chartData = new ArrayList<>();

		LocalDate today = LocalDate.now();

		if ("week".equals(filter)) {
			// Last 7 Days
			for (int i = 6; i >= 0; i--) {
				LocalDate d = today.minusDays(i);
				chartLabels.add(d.getDayOfWeek().toString().substring(0, 3)); // Mon, Tue...

				double dailySum = productOrderRepository.findByOrderDateBetween(d, d).stream()
						.filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).mapToDouble(ProductOrder::getPrice).sum();
				chartData.add(dailySum);
			}
		} else if ("month".equals(filter)) {
			// This Month
			int length = today.lengthOfMonth();
			for (int i = 1; i <= length; i++) {
				if (i > today.getDayOfMonth())
					break;

				LocalDate d = today.withDayOfMonth(i);
				chartLabels.add(String.valueOf(i));

				double dailySum = productOrderRepository.findByOrderDateBetween(d, d).stream()
						.filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).mapToDouble(ProductOrder::getPrice).sum();
				chartData.add(dailySum);
			}
		} else {
			// Default: Year (Last 12 Months)
			for (int i = 11; i >= 0; i--) {
				LocalDate d = today.minusMonths(i);
				String monthName = d.getMonth().toString();
				chartLabels.add(monthName.substring(0, 3)); // JAN, FEB...

				LocalDate startOfMonth = d.withDayOfMonth(1);
				LocalDate endOfMonth = d.withDayOfMonth(d.lengthOfMonth());

				double monthSum = productOrderRepository.findByOrderDateBetween(startOfMonth, endOfMonth).stream()
						.filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).mapToDouble(ProductOrder::getPrice).sum();
				chartData.add(monthSum);
			}
		}

		model.addAttribute("chartLabels", chartLabels);
		model.addAttribute("chartData", chartData);

		// --- C. CATEGORY PIE CHART LOGIC ---
		List<String> catNames = new ArrayList<>();
		List<Integer> catCounts = new ArrayList<>();

		for (Category cat : categoryRepository.findAll()) {
			catNames.add(cat.getName());
			catCounts.add(productRepository.findByCategoryId(cat.getId()).size());
		}

		model.addAttribute("catNames", catNames);
		model.addAttribute("catCounts", catCounts);

		return "admin-dashboard-files/dashboard-home";
	}

	// ===============================================
	// 3. PRODUCT MANAGEMENT
	// ===============================================
	@GetMapping("/products")
	public String productManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";
		List<Product> products = productRepository.findAll();
		model.addAttribute("products", products);
		return "admin-dashboard-files/product-management";
	}

	@GetMapping("/products/add")
	public String showAddProductPage(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";

		// Ensure this matches the name in th:object="${newProduct}"
		model.addAttribute("newProduct", new Product());
		model.addAttribute("categories", categoryRepository.findAllByOrderByIdDesc());

		return "admin-dashboard-files/admin-add-product";
	}

	@PostMapping("/products/add")
	public String saveProduct(@ModelAttribute("newProduct") Product product,
			@RequestParam("imageFile") MultipartFile file, RedirectAttributes redirectAttributes) {

		try {
			// 1. Save the Image File
			if (!file.isEmpty()) {
				String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images/";
				String fileName = file.getOriginalFilename();
				Path uploadPath = Paths.get(uploadDir);
				if (!Files.exists(uploadPath)) {
					Files.createDirectories(uploadPath);
				}
				try (java.io.InputStream inputStream = file.getInputStream()) {
					Path filePath = uploadPath.resolve(fileName);
					Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
				}
				product.setImageName(fileName);
			}

			// 2. Save the Product to DB first (so we get the generated ID)
			Product savedProduct = productRepository.save(product);

			// 3. ✅ NEW LOGIC: Calculate Expense (Unit Cost * Stock) and Save to StockLog
			if (savedProduct.getStock() > 0 && savedProduct.getCostPrice() != null && savedProduct.getCostPrice() > 0) {
				StockLog log = new StockLog();
				log.setProduct(savedProduct);
				log.setAction("RESTOCK"); // Important: This tag makes it show up as an Expense
				log.setQuantity(savedProduct.getStock());
				log.setUnitPrice(savedProduct.getCostPrice());

				// Calculate Total: 10 items * 80rs = 800rs
				double totalCost = savedProduct.getCostPrice() * savedProduct.getStock();
				log.setTotalAmount(totalCost);

				log.setTimestamp(LocalDateTime.now());
				stockLogRepository.save(log); // Save to database
			}

			redirectAttributes.addFlashAttribute("successMSG",
					"✅ Product Added & Expense of ₹" + (product.getCostPrice() * product.getStock()) + " Logged!");

		} catch (IOException e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("errorMSG", "❌ Error uploading image: " + e.getMessage());
		}
		return "redirect:/admin/products/add";
	}

	@GetMapping("/products/edit/{id}")
	public String editProduct(@PathVariable("id") Long id, Model model, HttpSession session) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";
		Product product = productRepository.findById(id).orElse(null);
		model.addAttribute("product", product);
		model.addAttribute("categories", categoryRepository.findAll());
		return "admin-dashboard-files/admin-edit-product";
	}

	@PostMapping("/products/update")
	public String updateProduct(@ModelAttribute("product") Product product) {
		productRepository.save(product);
		return "redirect:/admin/products";
	}

	// ... imports ...

	// ✅ UPDATED: Soft Delete Logic (Keeps Order History & Revenue)
	@GetMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
		try {
			Product product = productRepository.findById(id).orElse(null);

			if (product != null) {
				// Instead of deleting data, we mark it as "Deleted"
				product.setDeleted(true);

				// Optional: Set stock to 0 so no one can buy it anymore
				product.setStock(0);

				productRepository.save(product);

				ra.addFlashAttribute("successMSG",
						"✅ Product marked as deleted. Order history and Revenue data are SAFE.");
			}
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("errorMSG", "❌ Error deleting product: " + e.getMessage());
		}
		return "redirect:/admin/products";
	}

	// ===============================================
	// 4. EMPLOYEE MANAGEMENT
	// ===============================================
	@GetMapping("/employees")
	public String employeeManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";
		List<Employee> allEmployees = employeeRepository.findAll();
		model.addAttribute("employees", allEmployees);
		return "admin-dashboard-files/employee-management";
	}

	@GetMapping("/employees/approve/{email}")
	public String approveEmployee(@PathVariable String email) {
		Employee emp = employeeRepository.findById(email).orElse(null);
		if (emp != null) {
			emp.setStatus("ACTIVE");
			emp.setWorkStatus("FREE");
			employeeRepository.save(emp);
		}
		return "redirect:/admin/dashboard";
	}

	@GetMapping("/employees/delete/{email}")
	public String deleteEmployee(@PathVariable String email) {
		employeeRepository.deleteById(email);
		return "redirect:/admin/employees";
	}

	// ===============================================
	// 5. CUSTOMER ANALYTICS MANAGEMENT
	// ===============================================
	@GetMapping("/customers")
	public String customerManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";

		// 1. Get all regular users (Customers)
		List<User> allUsers = userRepository.findAll();
		List<CustomerAnalytics> customerStats = new java.util.ArrayList<>();

		// 2. Loop through each user and calculate stats
		for (User user : allUsers) {
			if ("USER".equalsIgnoreCase(user.getRole())) {
				// A. Calculate Product Stats
				List<ProductOrder> orders = productOrderRepository.findByUser(user);
				double productRevenue = orders.stream().filter(o -> "Paid".equalsIgnoreCase(o.getStatus()))
						.mapToDouble(ProductOrder::getPrice).sum();
				int productCount = orders.size();

				// B. Calculate Service Stats
				List<ServiceRequest> services = serviceRequestRepository.findByUser(user);
				double serviceRevenue = services.stream().filter(s -> s.getAmount() != null)
						.mapToDouble(ServiceRequest::getAmount).sum();
				int serviceCount = services.size();

				// C. Add to List
				customerStats
						.add(new CustomerAnalytics(user, productCount, productRevenue, serviceCount, serviceRevenue));
			}
		}

		model.addAttribute("customers", customerStats);
		return "admin-dashboard-files/customer-management";
	}

	// ===============================================
	// VIEW CUSTOMER DETAILS
	// ===============================================
	@GetMapping("/customers/view/{email}")
	public String viewCustomerDetails(@PathVariable("email") String email, Model model, HttpSession session) {
		if (session.getAttribute("adminSession") == null) {
			return "redirect:/admin/login";
		}

		User user = userRepository.findById(email).orElse(null);

		if (user != null) {
			List<ProductOrder> orders = productOrderRepository.findByUser(user);
			List<ServiceRequest> services = serviceRequestRepository.findByUser(user);

			model.addAttribute("customer", user);
			model.addAttribute("orders", orders);
			model.addAttribute("services", services);
		}

		return "admin-dashboard-files/customer-details";
	}

	// --- Helper Class for Analytics ---
	public static class CustomerAnalytics {
		private User user;
		private int productCount;
		private double productRevenue;
		private int serviceCount;
		private double serviceRevenue;
		private double grandTotal;

		public CustomerAnalytics(User user, int pCount, double pRev, int sCount, double sRev) {
			this.user = user;
			this.productCount = pCount;
			this.productRevenue = pRev;
			this.serviceCount = sCount;
			this.serviceRevenue = sRev;
			this.grandTotal = pRev + sRev;
		}

		public User getUser() {
			return user;
		}

		public int getProductCount() {
			return productCount;
		}

		public double getProductRevenue() {
			return productRevenue;
		}

		public int getServiceCount() {
			return serviceCount;
		}

		public double getServiceRevenue() {
			return serviceRevenue;
		}

		public double getGrandTotal() {
			return grandTotal;
		}
	}

	// ===============================================
	// 6. SERVICE REQUEST MANAGEMENT
	// ===============================================
	@GetMapping("/services")
	public String showServiceManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";
		List<ServiceRequest> requests = serviceRequestRepository.findAllByOrderByCreatedOnDesc();
		List<Employee> allEmployees = employeeRepository.findAll();
		model.addAttribute("requests", requests);
		model.addAttribute("employees", allEmployees);
		return "admin-dashboard-files/service-management";
	}

	@PostMapping("/services/assign")
	public String assignEmployeeToService(@RequestParam Long requestId, @RequestParam String employeeEmail,
			RedirectAttributes redirectAttributes) {
		ServiceRequest req = serviceRequestRepository.findById(requestId).orElse(null);
		Employee emp = employeeRepository.findById(employeeEmail).orElse(null);
		if (req != null && emp != null) {
			req.setAssignedEmployee(emp);
			req.setServiceStatus("Work Not Started");
			serviceRequestRepository.save(req);
			emp.setWorkStatus("BUSY");
			employeeRepository.save(emp);
			redirectAttributes.addFlashAttribute("msg", "Employee Assigned Successfully!");
		}
		return "redirect:/admin/services";
	}

	@PostMapping("/services/updateStatus")
	public String updateServiceStatus(@RequestParam Long requestId, @RequestParam String status,
			RedirectAttributes redirectAttributes) {
		ServiceRequest req = serviceRequestRepository.findById(requestId).orElse(null);
		if (req != null) {
			req.setServiceStatus(status);
			serviceRequestRepository.save(req);
			if ("Completed".equals(status) && req.getAssignedEmployee() != null) {
				Employee emp = req.getAssignedEmployee();
				emp.setWorkStatus("FREE");
				employeeRepository.save(emp);
			}
			redirectAttributes.addFlashAttribute("msg", "Status Updated to " + status);
		}
		return "redirect:/admin/services";
	}

	// ===============================================
	// 7. FEEDBACK & ORDERS
	// ===============================================
	@GetMapping("/feedback")
	public String showFeedbackManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";
		List<Feedback> allFeedback = feedbackRepository.findAll();
		model.addAttribute("feedbacks", allFeedback);
		return "admin-dashboard-files/feedback-management";
	}

	@GetMapping("/logout")
	public String adminLogout(HttpSession session) {
		session.invalidate();
		return "redirect:/admin/login";
	}

	// ===============================================
	// ORDER MANAGEMENT (With Filter Support)
	// ===============================================
	@GetMapping("/orders")
	public String showOrderManagement(@RequestParam(value = "status", required = false) String status,
			HttpSession session, Model model) {

		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";

		List<ProductOrder> orders;

		// Filter Logic
		if (status != null && !status.isEmpty()) {
			orders = productOrderRepository.findAll().stream()
					.filter(o -> status.equalsIgnoreCase(o.getDeliveryStatus())).toList();
			model.addAttribute("filterMsg", "Showing: " + status + " Orders");
		} else {
			orders = productOrderRepository.findAll();
		}

		model.addAttribute("orders", orders);
		model.addAttribute("employees", employeeRepository.findAll());
		return "admin-dashboard-files/order-management";
	}

	@PostMapping("/orders/assign")
	public String assignDeliveryBoy(@RequestParam Long orderId, @RequestParam String employeeEmail) {
		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		Employee emp = employeeRepository.findById(employeeEmail).orElse(null);
		if (order != null && emp != null) {
			order.setDeliveryBoy(emp);
			order.setDeliveryStatus("Out for Delivery");
			productOrderRepository.save(order);
			emp.setWorkStatus("BUSY");
			employeeRepository.save(emp);
		}
		return "redirect:/admin/orders";
	}

	// ===============================================
	// 8. SALES & ANALYTICS DASHBOARD
	// ===============================================
	// ===============================================
	// 8. SALES & ANALYTICS DASHBOARD (UPDATED WITH PREDICTION LOGIC)
	// ===============================================
	@GetMapping("/sales")
	public String showSalesDashboard(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";

		// --- 1. FINANCIAL CARDS (Existing Logic) ---
		List<ProductOrder> paidOrders = productOrderRepository.findAll().stream()
				.filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).toList();
		double productRevenue = paidOrders.stream().mapToDouble(ProductOrder::getPrice).sum();

		List<ServiceRequest> paidServices = serviceRequestRepository.findAll().stream()
				.filter(s -> s.getAmount() != null).toList();
		double serviceRevenue = paidServices.stream().mapToDouble(ServiceRequest::getAmount).sum();
		double totalRevenue = productRevenue + serviceRevenue;

		List<StockLog> expenses = stockLogRepository.findByActionOrderByTimestampDesc("RESTOCK");
		double totalExpenses = expenses.stream().mapToDouble(StockLog::getTotalAmount).sum();
		double netProfit = totalRevenue - totalExpenses;

		// --- 2. EMPLOYEE PERFORMANCE (Existing Logic) ---
		List<Employee> employees = employeeRepository.findAll();
		List<EmployeePerformance> empStats = new ArrayList<>();
		for (Employee emp : employees) {
			double earned = 0.0;
			int tasks = 0;
			if ("Delivery Boy".equalsIgnoreCase(emp.getJobTitle())) {
				earned = productOrderRepository.findAll().stream()
						.filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(emp.getEmail())
								&& "Paid".equalsIgnoreCase(o.getStatus()))
						.mapToDouble(ProductOrder::getPrice).sum();
				tasks = (int) productOrderRepository.findAll().stream()
						.filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(emp.getEmail())
								&& "Paid".equalsIgnoreCase(o.getStatus()))
						.count();
			} else {
				earned = serviceRequestRepository.findAll().stream()
						.filter(s -> s.getAssignedEmployee() != null
								&& s.getAssignedEmployee().getEmail().equals(emp.getEmail())
								&& "Completed".equalsIgnoreCase(s.getServiceStatus()))
						.mapToDouble(s -> s.getAmount() != null ? s.getAmount() : 0.0).sum();
				tasks = (int) serviceRequestRepository.findAll().stream()
						.filter(s -> s.getAssignedEmployee() != null
								&& s.getAssignedEmployee().getEmail().equals(emp.getEmail())
								&& "Completed".equalsIgnoreCase(s.getServiceStatus()))
						.count();
			}
			if (tasks > 0)
				empStats.add(new EmployeePerformance(emp, tasks, earned));
		}

		// --- 3. INVENTORY VELOCITY & PREDICTIONS (✅ NEW ANALYTICS FEATURE) ---
		// Logic: Calculate sales in last 30 days to find "Units Sold Per Day"
		LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

		// Get all products
		List<Product> allProducts = productRepository.findAll();
		List<InventoryAnalytics> analyticsList = new ArrayList<>();

		for (Product p : allProducts) {
			// Count items sold in last 30 days
			int soldLast30Days = productOrderRepository.findByOrderDateBetween(thirtyDaysAgo, LocalDate.now()).stream()
					.filter(o -> o.getProduct().getId().equals(p.getId()) && "Paid".equalsIgnoreCase(o.getStatus()))
					.mapToInt(ProductOrder::getQuantity).sum();

			// Only analyze if it has sold at least 1 item
			if (soldLast30Days > 0) {
				double dailyRate = soldLast30Days / 30.0; // Items sold per day
				int daysUntilStockout = (dailyRate > 0) ? (int) (p.getStock() / dailyRate) : 999;

				String status = "🟢 Stable";
				if (daysUntilStockout < 7)
					status = "🔴 Critical (Restock Now)";
				else if (daysUntilStockout < 15)
					status = "🟡 Fast Moving";

				analyticsList.add(new InventoryAnalytics(p.getName(), p.getStock(), soldLast30Days, dailyRate,
						daysUntilStockout, status));
			}
		}

		// Sort: Items running out soonest appear at the top
		analyticsList.sort((a, b) -> Integer.compare(a.daysUntilStockout, b.daysUntilStockout));

		model.addAttribute("totalRevenue", totalRevenue);
		model.addAttribute("totalExpenses", totalExpenses);
		model.addAttribute("netProfit", netProfit);
		model.addAttribute("stockLogs", expenses);
		model.addAttribute("empStats", empStats);
		model.addAttribute("products", productRepository.findAll());
		model.addAttribute("inventoryAnalytics", analyticsList); // ✅ Add to model

		return "admin-dashboard-files/sales-dashboard";
	}

	// ✅ HELPER CLASS FOR INVENTORY ANALYTICS (Paste this inside AdminController
	// class at the bottom)
	public static class InventoryAnalytics {
		public String productName;
		public int currentStock;
		public int soldLastMonth;
		public double velocity; // Items per day
		public int daysUntilStockout;
		public String status;

		public InventoryAnalytics(String name, int stock, int sold, double vel, int days, String stat) {
			this.productName = name;
			this.currentStock = stock;
			this.soldLastMonth = sold;
			this.velocity = vel;
			this.daysUntilStockout = days;
			this.status = stat;
		}
	}

	@PostMapping("/sales/restock")
	public String restockProduct(@RequestParam Long productId, @RequestParam int quantity,
			@RequestParam double buyingPrice, RedirectAttributes ra) {

		Product product = productRepository.findById(productId).orElse(null);
		if (product != null) {
			product.setStock(product.getStock() + quantity);
			productRepository.save(product);

			StockLog log = new StockLog();
			log.setProduct(product);
			log.setAction("RESTOCK");
			log.setQuantity(quantity);
			log.setTotalAmount(buyingPrice);
			log.setUnitPrice(buyingPrice / quantity);
			log.setTimestamp(LocalDateTime.now());
			stockLogRepository.save(log);

			ra.addFlashAttribute("msg", "✅ Restock Successful! Expense Logged.");
		}
		return "redirect:/admin/sales";
	}

	// ===============================================
	// ✅ DETAILED PDF REPORT GENERATION (Lifetime + Monthly Support)
	// ===============================================
	@GetMapping("/sales/report/pdf")
	public void exportToPDF(@RequestParam(value = "date", required = false) String dateString,
			HttpServletResponse response) throws IOException {

		// 1. DETERMINE DATE RANGE
		LocalDate startDate = null;
		LocalDate endDate = null;
		String reportTitle = "Lifetime Report"; // Default Title

		// If user selected a month, filter by that month
		if (dateString != null && !dateString.isEmpty()) {
			java.time.YearMonth selectedMonth = java.time.YearMonth.parse(dateString);
			startDate = selectedMonth.atDay(1);
			endDate = selectedMonth.atEndOfMonth();
			reportTitle = "Monthly Report - " + selectedMonth.getMonth() + " " + selectedMonth.getYear();
		}

		// 2. SET RESPONSE HEADERS
		response.setContentType("application/pdf");
		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=Mathapati_" + reportTitle.replace(" ", "_") + ".pdf";
		response.setHeader(headerKey, headerValue);

		// 3. GATHER DATA (With Filter Logic)

		// Final variables for lambda use
		final LocalDate finalStart = startDate;
		final LocalDate finalEnd = endDate;

		// A. Orders
		List<ProductOrder> orders = productOrderRepository.findAll().stream()
				.filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).filter(o -> {
					if (finalStart == null)
						return true; // No filter = All Time
					return o.getOrderDate() != null && !o.getOrderDate().isBefore(finalStart)
							&& !o.getOrderDate().isAfter(finalEnd);
				}).toList();

		// B. Services
		List<ServiceRequest> services = serviceRequestRepository.findAll().stream().filter(s -> s.getAmount() != null)
				.filter(s -> {
					if (finalStart == null)
						return true; // No filter = All Time

					LocalDate dateToCheck = s.getCompletionDate();
					if (dateToCheck == null && s.getCreatedOn() != null)
						dateToCheck = s.getCreatedOn().toLocalDate();

					return dateToCheck != null && !dateToCheck.isBefore(finalStart) && !dateToCheck.isAfter(finalEnd);
				}).toList();

		// C. Expenses
		List<StockLog> expenses = stockLogRepository.findByActionOrderByTimestampDesc("RESTOCK").stream().filter(l -> {
			if (finalStart == null)
				return true; // No filter = All Time
			return !l.getTimestamp().toLocalDate().isBefore(finalStart)
					&& !l.getTimestamp().toLocalDate().isAfter(finalEnd);
		}).toList();

		List<Employee> employees = employeeRepository.findAll();
		List<User> customers = userRepository.findAll();

		// 4. SETUP DOCUMENT
		Document document = new Document(PageSize.A4);
		PdfWriter.getInstance(document, response.getOutputStream());
		document.open();

		// Fonts
		Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(23, 27, 56));
		Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
		Font fontData = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

		// --- TITLE PAGE ---
		Paragraph title = new Paragraph("Mathapati Electricals - " + reportTitle, fontTitle);
		title.setAlignment(Paragraph.ALIGN_CENTER);
		document.add(title);

		if (startDate != null) {
			Paragraph dateParams = new Paragraph("Period: " + startDate + " to " + endDate, fontData);
			dateParams.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(dateParams);
		} else {
			Paragraph dateParams = new Paragraph("Period: All Time (Lifetime Data)", fontData);
			dateParams.setAlignment(Paragraph.ALIGN_CENTER);
			document.add(dateParams);
		}
		document.add(new Paragraph("\n"));

		// --- SECTION A: FINANCIAL SUMMARY ---
		double prodRev = orders.stream().mapToDouble(ProductOrder::getPrice).sum();
		double servRev = services.stream().mapToDouble(ServiceRequest::getAmount).sum();
		double totalExp = expenses.stream().mapToDouble(StockLog::getTotalAmount).sum();

		PdfPTable financialTable = new PdfPTable(2);
		financialTable.setWidthPercentage(100f);
		addHeaderCell(financialTable, "Metric", fontHeader);
		addHeaderCell(financialTable, "Amount (Rs.)", fontHeader);

		financialTable.addCell(new Phrase("Total Product Revenue", fontData));
		financialTable.addCell(new Phrase(String.valueOf(prodRev), fontData));

		financialTable.addCell(new Phrase("Total Service Revenue", fontData));
		financialTable.addCell(new Phrase(String.valueOf(servRev), fontData));

		financialTable.addCell(new Phrase("Stock Purchase Expenses", fontData));
		financialTable.addCell(new Phrase(String.valueOf(totalExp), fontData));

		PdfPCell profitCell = new PdfPCell(new Phrase("NET PROFIT", fontHeader));
		profitCell.setBackgroundColor(new Color(40, 167, 69)); // Green
		financialTable.addCell(profitCell);

		PdfPCell profitVal = new PdfPCell(new Phrase(String.valueOf((prodRev + servRev) - totalExp), fontHeader));
		profitVal.setBackgroundColor(new Color(40, 167, 69));
		financialTable.addCell(profitVal);

		document.add(new Paragraph("1. FINANCIAL SUMMARY", fontTitle));
		document.add(new Paragraph("\n"));
		document.add(financialTable);
		document.add(new Paragraph("\n"));

		// --- SECTION B: PRODUCT SALES BREAKDOWN ---
		document.add(new Paragraph("2. PRODUCT SALES DETAILS", fontTitle));
		document.add(new Paragraph("\n"));

		if (orders.isEmpty()) {
			document.add(new Paragraph("No products sold in this period.", fontData));
		} else {
			Map<String, int[]> productStats = new HashMap<>();
			for (ProductOrder o : orders) {
				String pName = o.getProduct().getName();
				productStats.putIfAbsent(pName, new int[] { 0, 0 });
				productStats.get(pName)[0] += o.getQuantity();
				productStats.get(pName)[1] += o.getPrice();
			}

			PdfPTable prodTable = new PdfPTable(3);
			prodTable.setWidthPercentage(100f);
			addHeaderCell(prodTable, "Product Name", fontHeader);
			addHeaderCell(prodTable, "Qty Sold", fontHeader);
			addHeaderCell(prodTable, "Revenue", fontHeader);

			for (Map.Entry<String, int[]> entry : productStats.entrySet()) {
				prodTable.addCell(new Phrase(entry.getKey(), fontData));
				prodTable.addCell(new Phrase(String.valueOf(entry.getValue()[0]), fontData));
				prodTable.addCell(new Phrase("Rs. " + entry.getValue()[1], fontData));
			}
			document.add(prodTable);
		}
		document.add(new Paragraph("\n"));

		// --- SECTION C: SERVICE REQUESTS BREAKDOWN ---
		document.add(new Paragraph("3. SERVICE REQUEST DETAILS", fontTitle));
		document.add(new Paragraph("\n"));

		if (services.isEmpty()) {
			document.add(new Paragraph("No service requests in this period.", fontData));
		} else {
			Map<String, int[]> serviceStats = new HashMap<>();
			for (ServiceRequest s : services) {
				String sType = s.getServiceType();
				serviceStats.putIfAbsent(sType, new int[] { 0, 0 });
				serviceStats.get(sType)[0]++;
				serviceStats.get(sType)[1] += s.getAmount();
			}

			PdfPTable servTable = new PdfPTable(3);
			servTable.setWidthPercentage(100f);
			addHeaderCell(servTable, "Service Type", fontHeader);
			addHeaderCell(servTable, "Count", fontHeader);
			addHeaderCell(servTable, "Revenue", fontHeader);

			for (Map.Entry<String, int[]> entry : serviceStats.entrySet()) {
				servTable.addCell(new Phrase(entry.getKey(), fontData));
				servTable.addCell(new Phrase(String.valueOf(entry.getValue()[0]), fontData));
				servTable.addCell(new Phrase("Rs. " + entry.getValue()[1], fontData));
			}
			document.add(servTable);
		}
		document.add(new Paragraph("\n"));

		// --- SECTION D: EMPLOYEE PERFORMANCE ---
		document.add(new Paragraph("4. EMPLOYEE PERFORMANCE", fontTitle));
		document.add(new Paragraph("\n"));

		PdfPTable empTable = new PdfPTable(3);
		empTable.setWidthPercentage(100f);
		addHeaderCell(empTable, "Employee Name", fontHeader);
		addHeaderCell(empTable, "Role", fontHeader);
		addHeaderCell(empTable, "Generated Revenue", fontHeader);

		boolean anyEmpActive = false;
		for (Employee emp : employees) {
			double earned = 0.0;
			if ("Delivery Boy".equalsIgnoreCase(emp.getJobTitle())) {
				earned = orders.stream()
						.filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(emp.getEmail()))
						.mapToDouble(ProductOrder::getPrice).sum();
			} else {
				earned = services.stream()
						.filter(s -> s.getAssignedEmployee() != null
								&& s.getAssignedEmployee().getEmail().equals(emp.getEmail()))
						.mapToDouble(ServiceRequest::getAmount).sum();
			}
			if (earned > 0) {
				anyEmpActive = true;
				empTable.addCell(new Phrase(emp.getFirstName() + " " + emp.getLastName(), fontData));
				empTable.addCell(new Phrase(emp.getJobTitle(), fontData));
				empTable.addCell(new Phrase("Rs. " + earned, fontData));
			}
		}

		if (!anyEmpActive) {
			document.add(new Paragraph("No employee activity recorded in this period.", fontData));
		} else {
			document.add(empTable);
		}
		document.add(new Paragraph("\n"));

		// --- SECTION E: CUSTOMER ACTIVITY ---
		document.add(new Paragraph("5. CUSTOMER ACTIVITY", fontTitle));
		document.add(new Paragraph("\n"));

		PdfPTable custTable = new PdfPTable(4);
		custTable.setWidthPercentage(100f);
		addHeaderCell(custTable, "Customer Name", fontHeader);
		addHeaderCell(custTable, "Items Bought", fontHeader);
		addHeaderCell(custTable, "Services", fontHeader);
		addHeaderCell(custTable, "Total Spent", fontHeader);

		boolean anyCustActive = false;
		for (User user : customers) {
			if (!"USER".equalsIgnoreCase(user.getRole()))
				continue;

			final String userEmail = user.getEmailid();

			long pCount = orders.stream().filter(o -> o.getUser().getEmailid().equals(userEmail)).count();
			double pSpent = orders.stream().filter(o -> o.getUser().getEmailid().equals(userEmail))
					.mapToDouble(ProductOrder::getPrice).sum();

			long sCount = services.stream().filter(s -> s.getUser().getEmailid().equals(userEmail)).count();
			double sSpent = services.stream().filter(s -> s.getUser().getEmailid().equals(userEmail))
					.mapToDouble(ServiceRequest::getAmount).sum();

			if (pSpent + sSpent > 0) {
				anyCustActive = true;
				custTable.addCell(new Phrase(user.getFirstName(), fontData));
				custTable.addCell(new Phrase(String.valueOf(pCount), fontData));
				custTable.addCell(new Phrase(String.valueOf(sCount), fontData));
				custTable.addCell(new Phrase("Rs. " + (pSpent + sSpent), fontData));
			}
		}

		if (!anyCustActive) {
			document.add(new Paragraph("No customer activity recorded in this period.", fontData));
		} else {
			document.add(custTable);
		}

		document.close();
	}

	// Helper Method for Table Styling
	private void addHeaderCell(PdfPTable table, String text, Font font) {
		PdfPCell cell = new PdfPCell(new Phrase(text, font));
		cell.setBackgroundColor(new Color(23, 27, 56));
		cell.setPadding(5);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
	}

	// Helper Class for Employee Table
	public static class EmployeePerformance {
		public Employee emp;
		public int tasksCompleted;
		public double totalGenerated;

		public EmployeePerformance(Employee e, int t, double g) {
			this.emp = e;
			this.tasksCompleted = t;
			this.totalGenerated = g;
		}
	}

	@PostMapping("/categories/add-quick")
	@ResponseBody // ✅ Change to ResponseBody to work with JavaScript Fetch
	public ResponseEntity<String> addCategoryQuickly(@RequestParam("catName") String name,
			@RequestParam("catImage") MultipartFile file) {
		if (name == null || name.trim().isEmpty()) {
			return ResponseEntity.badRequest().body("Name is required");
		}

		try {
			Category cat = new Category();
			cat.setName(name);

			// 1. Save the Image File to static/images/
			if (file != null && !file.isEmpty()) {
				String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images/";
				String fileName = file.getOriginalFilename();
				Path uploadPath = Paths.get(uploadDir);

				if (!Files.exists(uploadPath))
					Files.createDirectories(uploadPath);

				try (InputStream inputStream = file.getInputStream()) {
					Path filePath = uploadPath.resolve(fileName);
					Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
					cat.setImageName(fileName); // Save filename in DB
				}
			}

			// 2. Save Category to Database
			categoryRepository.save(cat);
			return ResponseEntity.ok("Success"); // ✅ Send success signal to JavaScript

		} catch (IOException e) {
			e.printStackTrace();
			return ResponseEntity.status(500).body("Error saving image");
		}
	}

	// ===============================================
	// 9. RETURN MANAGEMENT (New Feature)
	// ===============================================
	@GetMapping("/returns")
	public String showReturnManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";

		// Find all orders where a return has been requested
		List<ProductOrder> returns = productOrderRepository.findAll().stream().filter(o -> o.getReturnStatus() != null)
				.toList();

		List<Employee> deliveryBoys = employeeRepository.findAll().stream()
				.filter(e -> "Delivery Boy".equalsIgnoreCase(e.getJobTitle())).toList();

		model.addAttribute("returns", returns);
		model.addAttribute("deliveryBoys", deliveryBoys);
		return "admin-dashboard-files/admin-return-management";
	}

	@PostMapping("/returns/assign")
	public String assignReturnPickup(@RequestParam Long orderId, @RequestParam String employeeEmail,
			RedirectAttributes ra) {
		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		Employee emp = employeeRepository.findById(employeeEmail).orElse(null);

		if (order != null && emp != null) {
			order.setDeliveryBoy(emp);
			order.setReturnStatus("Pickup Assigned");
			productOrderRepository.save(order);

			emp.setWorkStatus("BUSY");
			employeeRepository.save(emp);

			ra.addFlashAttribute("msg", "✅ Delivery Boy Assigned for Pickup!");
		}
		return "redirect:/admin/returns";
	}

	@GetMapping("/returns/refund/{id}")
	public String processRefund(@PathVariable("id") Long orderId, RedirectAttributes ra) {
		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);

		if (order != null) {
			// 1. Mark as Refunded
			order.setReturnStatus("Refunded");

			// 2. Change Main Status to 'Returned'
			// (This automatically removes it from Revenue Calculation in Dashboard)
			order.setStatus("Returned");

			// 3. Free up the Delivery Boy
			if (order.getDeliveryBoy() != null) {
				Employee emp = order.getDeliveryBoy();
				emp.setWorkStatus("FREE");
				employeeRepository.save(emp);
			}

			productOrderRepository.save(order);
			ra.addFlashAttribute("msg", "✅ Refund Processed & Revenue Deducted!");
		}
		return "redirect:/admin/returns";
	}

	// ===============================================
	// GLOBAL ATTRIBUTES (Notification Badges)
	// ===============================================
	@ModelAttribute
	public void addGlobalAttributes(Model model, HttpSession session) {
		if (session.getAttribute("adminSession") != null) {
			// 1. Count Pending Orders
			long pendingOrderCount = productOrderRepository.countByDeliveryStatus("Pending");

			// 2. Count Pending Returns
			long returnRequestCount = productOrderRepository.countByReturnStatus("Requested");

			// 3. Count New Service Requests (assuming default status is null or 'Work Not
			// Started')
			// You might need to adjust the string string depending on your default status
			long pendingServiceCount = serviceRequestRepository.countByServiceStatus("Work Not Started");

			model.addAttribute("badgeOrders", pendingOrderCount);
			model.addAttribute("badgeReturns", returnRequestCount);
			model.addAttribute("badgeServices", pendingServiceCount);
		}
	}

	// ===============================================
	// 10. DAILY REPORT GENERATOR
	// ===============================================
	// ===============================================
	// 10. DAILY REPORT GENERATOR
	// ===============================================
	@GetMapping("/report/daily")
	public String generateDailyReport(@RequestParam(value = "reportDate", required = false) String dateString,
			jakarta.servlet.http.HttpSession session, // <--- 1. ADD THIS
			Model model) {

		// <--- 2. FIX THIS CHECK (Use 'session', not 'model')
		if (session.getAttribute("adminSession") == null) {
			return "redirect:/admin/login";
		}

		// 1. Determine Date (Default to Today if null)
		LocalDate selectedDate = (dateString != null && !dateString.isEmpty()) ? LocalDate.parse(dateString)
				: LocalDate.now();

		// 2. Fetch Orders for that Day
		List<ProductOrder> dailyOrders = productOrderRepository.findByOrderDate(selectedDate);
		double productRevenue = dailyOrders.stream().filter(o -> "Paid".equalsIgnoreCase(o.getStatus()))
				.mapToDouble(ProductOrder::getPrice).sum();
		long productsSold = dailyOrders.stream().filter(o -> "Paid".equalsIgnoreCase(o.getStatus()))
				.mapToInt(ProductOrder::getQuantity).sum();

		// 3. Fetch Services Completed that Day
		List<ServiceRequest> dailyServices = serviceRequestRepository.findByCompletionDate(selectedDate);
		double serviceRevenue = dailyServices.stream().mapToDouble(s -> s.getAmount() != null ? s.getAmount() : 0.0)
				.sum();
		long servicesDone = dailyServices.size();

		// 4. Fetch Restock/Expenses for that Day (Start of day to End of day)
		LocalDateTime startOfDay = selectedDate.atStartOfDay();
		LocalDateTime endOfDay = selectedDate.atTime(LocalTime.MAX);

		List<StockLog> dailyLogs = stockLogRepository.findByTimestampBetween(startOfDay, endOfDay);
		double dailyExpense = dailyLogs.stream().filter(l -> "RESTOCK".equalsIgnoreCase(l.getAction()))
				.mapToDouble(StockLog::getTotalAmount).sum();
		long itemsRestocked = dailyLogs.stream().filter(l -> "RESTOCK".equalsIgnoreCase(l.getAction()))
				.mapToInt(StockLog::getQuantity).sum();

		// 5. Total Daily Revenue
		double totalDailyRevenue = productRevenue + serviceRevenue;
		double netDailyProfit = totalDailyRevenue - dailyExpense;

		// 6. Add to Model
		model.addAttribute("reportDate", selectedDate);
		model.addAttribute("productRevenue", productRevenue);
		model.addAttribute("productsSold", productsSold);
		model.addAttribute("serviceRevenue", serviceRevenue);
		model.addAttribute("servicesDone", servicesDone);
		model.addAttribute("dailyExpense", dailyExpense);
		model.addAttribute("itemsRestocked", itemsRestocked);
		model.addAttribute("totalDailyRevenue", totalDailyRevenue);
		model.addAttribute("netDailyProfit", netDailyProfit);
		model.addAttribute("dailyOrders", dailyOrders);
		model.addAttribute("dailyServices", dailyServices);

		return "admin-dashboard-files/admin-daily-report";
	}
	// ==========================================
	// 11. DOWNLOAD DAILY REPORTS (Excel & PDF)
	// ==========================================

	@GetMapping("/report/daily/download/pdf")
	public void downloadDailyReportPdf(@RequestParam(value = "date", required = false) String dateString,
			HttpServletResponse response) throws IOException {

		LocalDate date = (dateString != null && !dateString.isEmpty()) ? LocalDate.parse(dateString) : LocalDate.now();

		response.setContentType("application/pdf");
		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=Daily_Report_" + date + ".pdf";
		response.setHeader(headerKey, headerValue);

		reportService.generateDailyPdf(date, response);
	}

	@GetMapping("/report/daily/download/excel")
	public void downloadDailyReportExcel(@RequestParam(value = "date", required = false) String dateString,
			HttpServletResponse response) throws IOException {

		LocalDate date = (dateString != null && !dateString.isEmpty()) ? LocalDate.parse(dateString) : LocalDate.now();

		response.setContentType("application/octet-stream");
		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=Daily_Report_" + date + ".xlsx";
		response.setHeader(headerKey, headerValue);

		reportService.generateDailyExcel(date, response);
	}
}