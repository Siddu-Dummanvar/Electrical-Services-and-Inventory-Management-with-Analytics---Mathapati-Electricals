package com.matpatielectricals.esinventoryanalytics.controller;

import java.util.List;

import java.util.ArrayList;
import java.util.Map; // ✅ Added
import java.util.HashMap; // ✅ Added
import java.time.LocalDate;
import java.nio.file.*;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.matpatielectricals.esinventoryanalytics.entities.*;
import com.matpatielectricals.esinventoryanalytics.repositories.*;

import jakarta.servlet.http.HttpSession;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.matpatielectricals.esinventoryanalytics.entities.StockLog;
import com.matpatielectricals.esinventoryanalytics.repositories.StockLogRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.awt.Color;

@Controller
@RequestMapping("/admin")
public class AdminController {

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
		model.addAttribute("newProduct", new Product());
		model.addAttribute("categories", categoryRepository.findAll());
		return "admin-dashboard-files/admin-add-product";
	}

	@PostMapping("/products/add")
	public String saveProduct(@ModelAttribute("newProduct") Product product,
			@RequestParam("imageFile") MultipartFile file, RedirectAttributes redirectAttributes) {

		try {
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
			productRepository.save(product);
			redirectAttributes.addFlashAttribute("successMSG", "✅ Product Added Successfully!");
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

	@GetMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable("id") Long id) {
		productRepository.deleteById(id);
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

	@GetMapping("/orders")
	public String showOrderManagement(HttpSession session, Model model) {
		if (session.getAttribute("adminSession") == null)
			return "redirect:/admin/login";
		List<ProductOrder> orders = productOrderRepository.findAll();
		List<Employee> employees = employeeRepository.findAll();
		model.addAttribute("orders", orders);
		model.addAttribute("employees", employees);
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
        if (session.getAttribute("adminSession") == null) return "redirect:/admin/login";

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
                    .filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(emp.getEmail()) && "Paid".equalsIgnoreCase(o.getStatus()))
                    .mapToDouble(ProductOrder::getPrice).sum();
                tasks = (int) productOrderRepository.findAll().stream()
                    .filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(emp.getEmail()) && "Paid".equalsIgnoreCase(o.getStatus())).count();
            } else {
                earned = serviceRequestRepository.findAll().stream()
                    .filter(s -> s.getAssignedEmployee() != null && s.getAssignedEmployee().getEmail().equals(emp.getEmail()) && "Completed".equalsIgnoreCase(s.getServiceStatus()))
                    .mapToDouble(s -> s.getAmount() != null ? s.getAmount() : 0.0).sum();
                tasks = (int) serviceRequestRepository.findAll().stream()
                    .filter(s -> s.getAssignedEmployee() != null && s.getAssignedEmployee().getEmail().equals(emp.getEmail()) && "Completed".equalsIgnoreCase(s.getServiceStatus())).count();
            }
            if(tasks > 0) empStats.add(new EmployeePerformance(emp, tasks, earned));
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
                if(daysUntilStockout < 7) status = "🔴 Critical (Restock Now)";
                else if(daysUntilStockout < 15) status = "🟡 Fast Moving";

                analyticsList.add(new InventoryAnalytics(p.getName(), p.getStock(), soldLast30Days, dailyRate, daysUntilStockout, status));
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

    // ✅ HELPER CLASS FOR INVENTORY ANALYTICS (Paste this inside AdminController class at the bottom)
    public static class InventoryAnalytics {
        public String productName;
        public int currentStock;
        public int soldLastMonth;
        public double velocity; // Items per day
        public int daysUntilStockout;
        public String status;

        public InventoryAnalytics(String name, int stock, int sold, double vel, int days, String stat) {
            this.productName = name; this.currentStock = stock; this.soldLastMonth = sold;
            this.velocity = vel; this.daysUntilStockout = days; this.status = stat;
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
            .filter(o -> "Paid".equalsIgnoreCase(o.getStatus()))
            .filter(o -> {
                if (finalStart == null) return true; // No filter = All Time
                return o.getOrderDate() != null && !o.getOrderDate().isBefore(finalStart) && !o.getOrderDate().isAfter(finalEnd);
            })
            .toList();

        // B. Services
        List<ServiceRequest> services = serviceRequestRepository.findAll().stream()
            .filter(s -> s.getAmount() != null)
            .filter(s -> {
                if (finalStart == null) return true; // No filter = All Time
                
                LocalDate dateToCheck = s.getCompletionDate();
                if(dateToCheck == null && s.getCreatedOn() != null) dateToCheck = s.getCreatedOn().toLocalDate();
                
                return dateToCheck != null && !dateToCheck.isBefore(finalStart) && !dateToCheck.isAfter(finalEnd);
            })
            .toList();

        // C. Expenses
        List<StockLog> expenses = stockLogRepository.findByActionOrderByTimestampDesc("RESTOCK").stream()
            .filter(l -> {
                if (finalStart == null) return true; // No filter = All Time
                return !l.getTimestamp().toLocalDate().isBefore(finalStart) && !l.getTimestamp().toLocalDate().isAfter(finalEnd);
            })
            .toList();

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
        
        if(startDate != null) {
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
        
        if(orders.isEmpty()) {
            document.add(new Paragraph("No products sold in this period.", fontData));
        } else {
            Map<String, int[]> productStats = new HashMap<>();
            for(ProductOrder o : orders) {
                String pName = o.getProduct().getName();
                productStats.putIfAbsent(pName, new int[]{0, 0});
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

        if(services.isEmpty()) {
            document.add(new Paragraph("No service requests in this period.", fontData));
        } else {
            Map<String, int[]> serviceStats = new HashMap<>(); 
            for(ServiceRequest s : services) {
                String sType = s.getServiceType();
                serviceStats.putIfAbsent(sType, new int[]{0, 0});
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
        for(Employee emp : employees) {
            double earned = 0.0;
            if ("Delivery Boy".equalsIgnoreCase(emp.getJobTitle())) {
                earned = orders.stream().filter(o -> o.getDeliveryBoy() != null && o.getDeliveryBoy().getEmail().equals(emp.getEmail()))
                        .mapToDouble(ProductOrder::getPrice).sum();
            } else {
                earned = services.stream().filter(s -> s.getAssignedEmployee() != null && s.getAssignedEmployee().getEmail().equals(emp.getEmail()))
                        .mapToDouble(ServiceRequest::getAmount).sum();
            }
            if(earned > 0) {
                anyEmpActive = true;
                empTable.addCell(new Phrase(emp.getFirstName() + " " + emp.getLastName(), fontData));
                empTable.addCell(new Phrase(emp.getJobTitle(), fontData));
                empTable.addCell(new Phrase("Rs. " + earned, fontData));
            }
        }
        
        if(!anyEmpActive) {
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
        for(User user : customers) {
            if(!"USER".equalsIgnoreCase(user.getRole())) continue;
            
            final String userEmail = user.getEmailid(); 

            long pCount = orders.stream().filter(o -> o.getUser().getEmailid().equals(userEmail)).count();
            double pSpent = orders.stream().filter(o -> o.getUser().getEmailid().equals(userEmail)).mapToDouble(ProductOrder::getPrice).sum();
            
            long sCount = services.stream().filter(s -> s.getUser().getEmailid().equals(userEmail)).count();
            double sSpent = services.stream().filter(s -> s.getUser().getEmailid().equals(userEmail)).mapToDouble(ServiceRequest::getAmount).sum();

            if(pSpent + sSpent > 0) {
                anyCustActive = true;
                custTable.addCell(new Phrase(user.getFirstName(), fontData));
                custTable.addCell(new Phrase(String.valueOf(pCount), fontData));
                custTable.addCell(new Phrase(String.valueOf(sCount), fontData));
                custTable.addCell(new Phrase("Rs. " + (pSpent + sSpent), fontData));
            }
        }
        
        if(!anyCustActive) {
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
}