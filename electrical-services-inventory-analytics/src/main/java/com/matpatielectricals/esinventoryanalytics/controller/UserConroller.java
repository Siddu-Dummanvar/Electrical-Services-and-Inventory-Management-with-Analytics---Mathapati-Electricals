package com.matpatielectricals.esinventoryanalytics.controller;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
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
import com.matpatielectricals.esinventoryanalytics.entities.Cart;
import com.matpatielectricals.esinventoryanalytics.entities.Category;
import com.matpatielectricals.esinventoryanalytics.entities.Feedback;
import com.matpatielectricals.esinventoryanalytics.entities.Product;
import com.matpatielectricals.esinventoryanalytics.entities.ProductOrder;
import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import com.matpatielectricals.esinventoryanalytics.entities.StockLog;
import com.matpatielectricals.esinventoryanalytics.entities.User;
import com.matpatielectricals.esinventoryanalytics.repositories.CartRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.CategoryRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.FeedbackRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductOrderRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.ServiceRequestRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.StockLogRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.UserRepository;
import com.matpatielectricals.esinventoryanalytics.services.CartService;
import com.matpatielectricals.esinventoryanalytics.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class UserConroller {

	@Autowired
	private ProductOrderRepository productOrderRepository; // ✅ Only declared once now

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private CartService cartService;

	@Autowired
	private UserServices userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private FeedbackRepository feedbackRepository;

	@Autowired
	private StockLogRepository stockLogRepository;

	private static final Logger log = LoggerFactory.getLogger(UserConroller.class);

	// --- PUBLIC PAGES ---

	@GetMapping({ "/", "/LandingPage" })
	public String openLandingPage() {
		return "LandingPage";
	}

	@GetMapping("/user-register")
	public String openUserRegisterPage(Model model) {
		model.addAttribute("user", new User());
		return "user-register";
	}

	@GetMapping("/user-Login")
	public String openUserLoginPage() {
		return "User-Login";
	}

	// --- PROTECTED USER PAGES ---

	@GetMapping("/about-me")
	public String openAboutMePage(Model model, HttpSession session) {
		User loggedInUser = (User) session.getAttribute("sessionUser");
		if (loggedInUser == null)
			return "redirect:/user-Login";
		model.addAttribute("sessionUser", loggedInUser);
		return "userpage/about-me";
	}

	@GetMapping("/index")
	public String openUserHomePage(@RequestParam(value = "keyword", required = false) String keyword,
			HttpSession session, Model model) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";

		List<Category> allCategories;

		if (keyword != null && !keyword.trim().isEmpty()) {
			allCategories = categoryRepository.findByNameContainingIgnoreCase(keyword);
		} else {
			allCategories = categoryRepository.findAllByOrderByIdDesc();
		}

		model.addAttribute("categories", allCategories);
		model.addAttribute("keyword", keyword);
		model.addAttribute("newFeedback", new Feedback());

		return "index";
	}

	// --- FEEDBACK ---
	@PostMapping("/submitFeedback")
	public String saveFeedback(@ModelAttribute("newFeedback") Feedback feedback, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User user = (User) session.getAttribute("sessionUser");
		if (user == null)
			return "redirect:/user-Login";

		feedback.setUser(user);
		feedbackRepository.save(feedback);
		redirectAttributes.addFlashAttribute("feedbackMsg", "Thank you! Your feedback has been submitted.");
		return "redirect:/index";
	}

	// --- PRODUCTS & SERVICES ---
	@GetMapping("/products/category/{id}")
	public String showProductsByCategory(@PathVariable("id") Long categoryId, HttpSession session, Model model) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";
		Category category = categoryRepository.findById(categoryId).orElse(null);

		// ✅ UPDATED: Only fetch active products (hides deleted ones)
		List<Product> products = productRepository.findByCategoryIdAndDeletedFalse(categoryId);

		if (category == null)
			return "redirect:/index";

		model.addAttribute("category", category);
		model.addAttribute("products", products);
		return "products/product-list";
	}

	@GetMapping("/services")
	public String showServicesPage(HttpSession session, Model model) {
		User user = (User) session.getAttribute("sessionUser");
		if (user != null) {
			model.addAttribute("user", user);
		}
		return "services";
	}

	// --- PROFILE & CART ---
	@GetMapping("/user-profile")
	public String userProfile(Model model, HttpSession session) {
		User user = (User) session.getAttribute("sessionUser");
		if (user == null)
			return "redirect:/user-Login";

		List<Cart> cartItems = cartRepository.findByUser(user);
		List<ProductOrder> myorders = productOrderRepository.findByUser(user);

		model.addAttribute("sessionUser", user);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("myorders", myorders);

		return "user-profile";
	}

	@GetMapping("/cart/remove/{id}")
	public String removeCartItem(@PathVariable Integer id, HttpSession session) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";
		cartRepository.deleteById(id);
		return "redirect:/cart";
	}

	@GetMapping("/cart")
	public String openCartPage(Model model, HttpSession session) {
		User sessionUser = (User) session.getAttribute("sessionUser");
		if (sessionUser == null)
			return "redirect:/user-Login";

		User dbUser = userRepository.findByEmailid(sessionUser.getEmailid());
		List<Cart> cartItems = cartRepository.findByUser(dbUser);

		double totalOrderPrice = 0.0;
		java.util.Iterator<Cart> iterator = cartItems.iterator();
		while (iterator.hasNext()) {
			Cart item = iterator.next();
			if (item.getProduct() == null) {
				cartRepository.delete(item);
				iterator.remove();
				continue;
			}
			Double price = item.getProduct().getSellingPrice();
			if (price == null)
				price = 0.0;
			totalOrderPrice += (price * item.getQuantity());
		}

		model.addAttribute("cartItems", cartItems);
		model.addAttribute("totalOrderPrice", totalOrderPrice);
		return "cart";
	}

	@GetMapping("/addToCart/{id}/{qty}")
	public String addToCartWithQuantity(@PathVariable("id") Long productId, @PathVariable("qty") int quantity,
			HttpSession session, RedirectAttributes redirectAttributes, HttpServletRequest request) {
		User user = (User) session.getAttribute("sessionUser");
		if (user == null)
			return "redirect:/user-Login";

		cartService.addToCart(user, productId, quantity);
		redirectAttributes.addFlashAttribute("successMSG", "✅ Added " + quantity + " items to cart!");
		String referer = request.getHeader("Referer");
		return "redirect:" + (referer != null ? referer : "/index");
	}

	// --- AUTHENTICATION ---
	@PostMapping("/Loginform")
	public String handleLoginForm(@ModelAttribute("user") User user, Model model, HttpSession session) {
		boolean isAuthenticated = userService.loginUserService(user.getEmailid(), user.getPassword());
		if (isAuthenticated) {
			User authenticatedUser = userRepository.findByEmailid(user.getEmailid());
			session.setAttribute("sessionUser", authenticatedUser);
			model.addAttribute("sessionUser", authenticatedUser);
			return "redirect:/index";
		} else {
			model.addAttribute("errorMSG", "❌ Incorrect Email ID or Password!");
			return "User-Login";
		}
	}

	@PostMapping("/regForm")
	public String handleRegForm(@Valid @ModelAttribute("user") User user, BindingResult result, Model model) {
		if (result.hasErrors()) {
			model.addAttribute("errorMSG", "❌ Please fix the highlighted errors and try again.");
			return "user-register";
		}
		try {
			userService.registerUserService(user);
			model.addAttribute("successMSG", "✅ Registration completed successfully!");
			model.addAttribute("user", new User());
		} catch (DataIntegrityViolationException e) {
			model.addAttribute("errorMSG", "❌ This email ID is already registered!");
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("errorMSG", "❌ Registration failed due to an internal error.");
		}
		return "user-register";
	}

	// --- ORDER SAVING & RECEIPT ---

	@PostMapping("/save_single_order")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> saveSingleOrder(@RequestBody Map<String, Object> data,
			HttpSession session) {
		Map<String, Object> response = new HashMap<>();
		User user = (User) session.getAttribute("sessionUser");
		if (user == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		try {
			Long productId = Long.parseLong(data.get("productId").toString());
			int quantity = Integer.parseInt(data.get("quantity").toString());
			double amount = Double.parseDouble(data.get("amount").toString());
			String paymentMode = (String) data.get("paymentMode");
			String paymentStatus = (String) data.get("paymentStatus");
			String txnId = (String) data.get("transactionId");

			Product product = productRepository.findById(productId).orElse(null);
			if (product == null)
				return ResponseEntity.badRequest().build();

			// 1. Check if enough stock is available
			if (product.getStock() < quantity) {
				return ResponseEntity.badRequest().body(Map.of("status", "No Stock"));
			}

			// 2. Create and Save Order
			ProductOrder order = new ProductOrder();
			order.setUser(user);
			order.setProduct(product);
			order.setQuantity(quantity);
			order.setPrice(amount);
			order.setPaymentType(paymentMode);
			order.setStatus(paymentStatus);
			order.setTransactionId(txnId);
			order.setOrderDate(LocalDate.now());
			order.setDeliveryStatus("Pending");
			ProductOrder savedOrder = productOrderRepository.save(order);

			// 3. ✅ DECREASE STOCK
			product.setStock(product.getStock() - quantity);
			productRepository.save(product);

			// 4. ✅ CREATE STOCK LOG (For Analytics)
			StockLog log = new StockLog();
			log.setProduct(product);
			log.setAction("SALE");
			log.setQuantity(quantity);
			log.setUnitPrice(product.getSellingPrice()); // Sold at Selling Price
			log.setTotalAmount(quantity * product.getSellingPrice()); // Revenue

			// Calculate Profit (Selling Price - Cost Price)
			double cost = (product.getCostPrice() != null) ? product.getCostPrice() : 0.0;
			log.setProfit((product.getSellingPrice() - cost) * quantity);

			log.setTimestamp(java.time.LocalDateTime.now());
			stockLogRepository.save(log);

			response.put("status", "success");
			response.put("orderId", savedOrder.getId());
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	// CORRECT Mapping for Receipt
	@GetMapping("/order-receipt/{id}")
	public String showOrderReceipt(@PathVariable("id") Long orderId, Model model, HttpSession session) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";

		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		if (order == null)
			return "redirect:/index";

		model.addAttribute("order", order);
		model.addAttribute("today", LocalDate.now());

		return "receipt";
	}

	@GetMapping("/myorder")
	public String myrders(Model model, HttpSession session) {
		User user = (User) session.getAttribute("sessionUser");
		if (user == null)
			return "redirect:/user-Login";
		List<ProductOrder> myorders = productOrderRepository.findByUser(user);
		model.addAttribute("myorders", myorders);
		return "myorder";
	}

	// ==========================================
	// DOWNLOAD INVOICE PDF
	// ==========================================
	@GetMapping("/order/invoice/{id}")
	public void downloadInvoice(@PathVariable("id") Long orderId, HttpServletResponse response) throws IOException {

		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		if (order == null)
			return;

		// 1. Set PDF Response Headers
		response.setContentType("application/pdf");
		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=Invoice_" + order.getId() + ".pdf";
		response.setHeader(headerKey, headerValue);

		// 2. Create Document
		Document document = new Document(PageSize.A4);
		PdfWriter.getInstance(document, response.getOutputStream());

		document.open();

		// 3. Add Content
		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
		Paragraph title = new Paragraph("INVOICE - MATHAPATI ELECTRICALS", titleFont);
		title.setAlignment(Element.ALIGN_CENTER);
		document.add(title);

		document.add(new Paragraph("\n"));

		Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
		document.add(new Paragraph("Order ID: #" + order.getId(), font));
		document.add(new Paragraph("Date: " + order.getOrderDate(), font));
		document.add(new Paragraph("Customer: " + order.getUser().getFirstName() + " " + order.getUser().getLastName(),
				font));
		document.add(new Paragraph("Email: " + order.getUser().getEmailid(), font));

		document.add(new Paragraph("\n"));

		PdfPTable table = new PdfPTable(4);
		table.setWidthPercentage(100);
		table.setSpacingBefore(10);

		PdfPCell cell = new PdfPCell();
		cell.setBackgroundColor(Color.LIGHT_GRAY);
		cell.setPadding(5);

		cell.setPhrase(new Phrase("Product", font));
		table.addCell(cell);
		cell.setPhrase(new Phrase("Quantity", font));
		table.addCell(cell);
		cell.setPhrase(new Phrase("Unit Price", font));
		table.addCell(cell);
		cell.setPhrase(new Phrase("Total", font));
		table.addCell(cell);

		table.addCell(order.getProduct().getName());
		table.addCell(String.valueOf(order.getQuantity()));
		table.addCell("Rs. " + order.getProduct().getSellingPrice());
		table.addCell("Rs. " + order.getPrice());

		document.add(table);

		document.add(new Paragraph("\n"));
		Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
		Paragraph totalPara = new Paragraph("Grand Total: Rs. " + order.getPrice(), totalFont);
		totalPara.setAlignment(Element.ALIGN_RIGHT);
		document.add(totalPara);

		document.add(new Paragraph("\n\n"));
		Paragraph footer = new Paragraph("Thank you for shopping with Mathapati Electricals!",
				FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
		footer.setAlignment(Element.ALIGN_CENTER);
		document.add(footer);

		document.close();
	}

	@GetMapping("/order-confirmation/{id}")
	public String showOrderSuccessPage(@PathVariable("id") Long orderId, Model model, HttpSession session) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";

		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
		if (order == null)
			return "redirect:/index";

		model.addAttribute("order", order);
		return "order-success";
	}

	// ==========================================
	// 1. SAVE BULK ORDER (Cart -> Orders)
	// ==========================================
	@PostMapping("/save_bulk_order")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> saveBulkOrder(@RequestBody Map<String, Object> data,
			HttpSession session) {
		Map<String, Object> response = new HashMap<>();
		User user = (User) session.getAttribute("sessionUser");
		if (user == null)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		String txnId = (String) data.get("transactionId");
		String paymentMode = (String) data.get("paymentMode");
		String paymentStatus = (String) data.get("paymentStatus");

		List<Cart> cartItems = cartRepository.findByUser(user);

		if (cartItems.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("status", "empty"));
		}

		for (Cart item : cartItems) {
			ProductOrder order = new ProductOrder();
			order.setUser(user);
			order.setProduct(item.getProduct());
			order.setQuantity(item.getQuantity());
			order.setPrice(item.getProduct().getSellingPrice() * item.getQuantity());
			order.setPaymentType(paymentMode);
			order.setStatus(paymentStatus);
			order.setTransactionId(txnId);
			order.setOrderDate(LocalDate.now());
			order.setDeliveryStatus("Pending");

			productOrderRepository.save(order);

			Product p = item.getProduct();
			p.setStock(p.getStock() - item.getQuantity());
			productRepository.save(p);
		}

		cartRepository.deleteAll(cartItems);

		response.put("status", "success");
		response.put("transactionId", txnId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/order-confirmation/txn/{txnId}")
	public String showBulkOrderSuccessPage(@PathVariable("txnId") String txnId, Model model, HttpSession session) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";

		List<ProductOrder> orders = productOrderRepository.findByTransactionId(txnId);
		if (orders.isEmpty())
			return "redirect:/index";

		model.addAttribute("orderSummary", orders.get(0));
		model.addAttribute("totalItems", orders.size());

		double grandTotal = orders.stream().mapToDouble(ProductOrder::getPrice).sum();
		model.addAttribute("grandTotal", grandTotal);

		return "order-success-bulk";
	}

	@GetMapping("/order/invoice/txn/{txnId}")
	public void downloadBulkInvoice(@PathVariable("txnId") String txnId, HttpServletResponse response)
			throws IOException {
		List<ProductOrder> orders = productOrderRepository.findByTransactionId(txnId);
		if (orders.isEmpty())
			return;

		User user = orders.get(0).getUser();

		response.setContentType("application/pdf");
		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=Invoice_" + txnId + ".pdf";
		response.setHeader(headerKey, headerValue);

		Document document = new Document(PageSize.A4);
		PdfWriter.getInstance(document, response.getOutputStream());

		document.open();

		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
		Paragraph title = new Paragraph("INVOICE - MATHAPATI ELECTRICALS", titleFont);
		title.setAlignment(Element.ALIGN_CENTER);
		document.add(title);

		document.add(new Paragraph("\n"));

		Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
		document.add(new Paragraph("Transaction ID: " + txnId, font));
		document.add(new Paragraph("Date: " + LocalDate.now(), font));
		document.add(new Paragraph("Customer: " + user.getFirstName() + " " + user.getLastName(), font));

		document.add(new Paragraph("\n"));

		PdfPTable table = new PdfPTable(4);
		table.setWidthPercentage(100);

		table.addCell(new Phrase("Product", font));
		table.addCell(new Phrase("Qty", font));
		table.addCell(new Phrase("Price (Each)", font));
		table.addCell(new Phrase("Total", font));

		double grandTotal = 0;

		for (ProductOrder order : orders) {
			table.addCell(order.getProduct().getName());
			table.addCell(String.valueOf(order.getQuantity()));
			table.addCell("Rs. " + order.getProduct().getSellingPrice());
			table.addCell("Rs. " + order.getPrice());
			grandTotal += order.getPrice();
		}

		document.add(table);

		document.add(new Paragraph("\n"));
		Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
		Paragraph totalPara = new Paragraph("Grand Total: Rs. " + grandTotal, totalFont);
		totalPara.setAlignment(Element.ALIGN_RIGHT);
		document.add(totalPara);

		document.close();
	}

	@GetMapping("/my-services")
	public String showMyServices(Model model, HttpSession session) {
		User user = (User) session.getAttribute("sessionUser");
		if (user == null) {
			return "redirect:/user-Login";
		}

		List<ServiceRequest> myServices = serviceRequestRepository.findByUser(user);
		model.addAttribute("myServices", myServices);
		return "my-services";
	}

	// ✅ ONLY ONE RETURN REQUEST METHOD NOW
	@PostMapping("/user/return-request")
	public String returnProductRequest(@RequestParam("orderId") Long orderId, @RequestParam("reason") String reason,
			HttpSession session, RedirectAttributes ra) {

		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";

		ProductOrder order = productOrderRepository.findById(orderId).orElse(null);

		if (order != null && "Delivered".equals(order.getDeliveryStatus())) {

			LocalDate deliveryDate = order.getDeliveryDate();
			if (deliveryDate != null && LocalDate.now().isAfter(deliveryDate.plusDays(2))) {
				ra.addFlashAttribute("errorMSG", "❌ Return period expired! Returns only allowed within 2 days.");
				return "redirect:/myorder";
			}

			order.setReturnStatus("Requested");
			order.setReturnReason(reason); // ✅ Save Reason
			productOrderRepository.save(order);

			ra.addFlashAttribute("successMSG", "✅ Return Request Submitted! Admin will assign a pickup agent.");
		} else {
			ra.addFlashAttribute("errorMSG", "❌ Invalid Order.");
		}
		return "redirect:/myorder";
	}
	// ===============================================
    // USER LOGOUT
    // ===============================================
    @GetMapping("/user-logout")
    public String userLogout(HttpSession session) {
        session.invalidate(); // Destroys the session (logs out)
        return "redirect:/LandingPage"; // Redirects to Landing Page
    }
}