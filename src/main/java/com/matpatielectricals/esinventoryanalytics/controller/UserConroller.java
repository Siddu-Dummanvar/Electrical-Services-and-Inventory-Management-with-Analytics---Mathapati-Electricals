package com.matpatielectricals.esinventoryanalytics.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.awt.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.matpatielectricals.esinventoryanalytics.entities.*;
import com.matpatielectricals.esinventoryanalytics.repositories.*;
import com.matpatielectricals.esinventoryanalytics.services.CartService;
import com.matpatielectricals.esinventoryanalytics.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class UserConroller {

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
	private ProductOrderRepository productOrderRepository;

	private static final Logger log = LoggerFactory.getLogger(UserConroller.class);

	
	@Autowired private StockLogRepository stockLogRepository;
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

	@GetMapping("/Home")
	public String openUserHomePage(HttpSession session, Model model) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";

		List<Category> allCategories = categoryRepository.findAll();
		model.addAttribute("categories", allCategories);
		model.addAttribute("newFeedback", new Feedback());

		return "Home";
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
		return "redirect:/Home";
	}

	// --- PRODUCTS & SERVICES ---
	@GetMapping("/products/category/{id}")
	public String showProductsByCategory(@PathVariable("id") Long categoryId, HttpSession session, Model model) {
		if (session.getAttribute("sessionUser") == null)
			return "redirect:/user-Login";
		Category category = categoryRepository.findById(categoryId).orElse(null);
		List<Product> products = productRepository.findByCategoryId(categoryId);

		if (category == null)
			return "redirect:/Home";

		model.addAttribute("category", category);
		model.addAttribute("products", products);
		return "products/product-list";
	}

	@GetMapping("/services")
	public String showServicesPage(HttpSession session, Model model) {
	    // Get logged-in user
	    User user = (User) session.getAttribute("sessionUser");
	    
	    // Pass user to the HTML page for auto-filling
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
		return "redirect:" + (referer != null ? referer : "/Home");
	}

	// --- AUTHENTICATION ---
	@PostMapping("/Loginform")
	public String handleLoginForm(@ModelAttribute("user") User user, Model model, HttpSession session) {
		boolean isAuthenticated = userService.loginUserService(user.getEmailid(), user.getPassword());
		if (isAuthenticated) {
			User authenticatedUser = userRepository.findByEmailid(user.getEmailid());
			session.setAttribute("sessionUser", authenticatedUser);
			model.addAttribute("sessionUser", authenticatedUser);
			return "redirect:/Home";
		} else {
		    model.addAttribute("errorMSG", "❌ Incorrect Email ID or Password!");
		    return "user-Login";
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
    public ResponseEntity<Map<String, Object>> saveSingleOrder(@RequestBody Map<String, Object> data, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("sessionUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            Long productId = Long.parseLong(data.get("productId").toString());
            int quantity = Integer.parseInt(data.get("quantity").toString());
            double amount = Double.parseDouble(data.get("amount").toString());
            String paymentMode = (String) data.get("paymentMode");
            String paymentStatus = (String) data.get("paymentStatus");
            String txnId = (String) data.get("transactionId");

            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) return ResponseEntity.badRequest().build();

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
			return "redirect:/Home";

		model.addAttribute("order", order);
		model.addAttribute("today", LocalDate.now());

		return "receipt"; // Looks for receipt.html
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
        if (order == null) return;

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
        // -- Title --
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
        Paragraph title = new Paragraph("INVOICE - MATHAPATI ELECTRICALS", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph("\n")); // Space

        // -- Customer & Order Info --
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        document.add(new Paragraph("Order ID: #" + order.getId(), font));
        document.add(new Paragraph("Date: " + order.getOrderDate(), font));
        document.add(new Paragraph("Customer: " + order.getUser().getFirstName() + " " + order.getUser().getLastName(), font));
        document.add(new Paragraph("Email: " + order.getUser().getEmailid(), font));
        
        document.add(new Paragraph("\n")); // Space

        // -- Table for Product Details --
        PdfPTable table = new PdfPTable(4); // 4 Columns
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);
        
        // Table Headers
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setPadding(5);
        
        cell.setPhrase(new Phrase("Product", font)); table.addCell(cell);
        cell.setPhrase(new Phrase("Quantity", font)); table.addCell(cell);
        cell.setPhrase(new Phrase("Unit Price", font)); table.addCell(cell);
        cell.setPhrase(new Phrase("Total", font)); table.addCell(cell);

        // Table Data
        table.addCell(order.getProduct().getName());
        table.addCell(String.valueOf(order.getQuantity()));
        table.addCell("Rs. " + order.getProduct().getSellingPrice());
        table.addCell("Rs. " + order.getPrice());

        document.add(table);

        // -- Grand Total --
        document.add(new Paragraph("\n"));
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph totalPara = new Paragraph("Grand Total: Rs. " + order.getPrice(), totalFont);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(totalPara);
        
        // -- Footer --
        document.add(new Paragraph("\n\n"));
        Paragraph footer = new Paragraph("Thank you for shopping with Mathapati Electricals!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        document.close();
    }
 // ==========================================
    // SHOW ORDER SUCCESS PAGE (With Invoice Button)
    // ==========================================
    @GetMapping("/order-confirmation/{id}")
    public String showOrderSuccessPage(@PathVariable("id") Long orderId, Model model, HttpSession session) {
        if (session.getAttribute("sessionUser") == null) return "redirect:/user-Login";

        ProductOrder order = productOrderRepository.findById(orderId).orElse(null);
        if (order == null) return "redirect:/Home";

        model.addAttribute("order", order);
        return "order-success"; // Loads order-success.html
    }
    
 // ==========================================
    // 1. SAVE BULK ORDER (Cart -> Orders)
    // ==========================================
    @PostMapping("/save_bulk_order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveBulkOrder(@RequestBody Map<String, Object> data, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        User user = (User) session.getAttribute("sessionUser");
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String txnId = (String) data.get("transactionId");
        String paymentMode = (String) data.get("paymentMode");
        String paymentStatus = (String) data.get("paymentStatus");
        
        // Get all cart items for this user
        List<Cart> cartItems = cartRepository.findByUser(user);
        
        if(cartItems.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "empty"));
        }

        // Convert Cart Items to Product Orders
        for (Cart item : cartItems) {
            ProductOrder order = new ProductOrder();
            order.setUser(user);
            order.setProduct(item.getProduct());
            order.setQuantity(item.getQuantity());
            // Calculate price based on quantity
            order.setPrice(item.getProduct().getSellingPrice() * item.getQuantity()); 
            order.setPaymentType(paymentMode);
            order.setStatus(paymentStatus);
            order.setTransactionId(txnId);
            order.setOrderDate(LocalDate.now());
            order.setDeliveryStatus("Pending");
            
            productOrderRepository.save(order);
            
            // Decrease Stock
            Product p = item.getProduct();
            p.setStock(p.getStock() - item.getQuantity());
            productRepository.save(p);
        }

        // Clear Cart
        cartRepository.deleteAll(cartItems);

        response.put("status", "success");
        response.put("transactionId", txnId); // Return Txn ID for Invoice
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. SHOW BULK SUCCESS PAGE
    // ==========================================
    @GetMapping("/order-confirmation/txn/{txnId}")
    public String showBulkOrderSuccessPage(@PathVariable("txnId") String txnId, Model model, HttpSession session) {
        if (session.getAttribute("sessionUser") == null) return "redirect:/user-Login";

        List<ProductOrder> orders = productOrderRepository.findByTransactionId(txnId);
        if (orders.isEmpty()) return "redirect:/Home";

        // Pass the FIRST order just to get basic details (Date, User info)
        model.addAttribute("orderSummary", orders.get(0)); 
        model.addAttribute("totalItems", orders.size());
        
        // Calculate Grand Total
        double grandTotal = orders.stream().mapToDouble(ProductOrder::getPrice).sum();
        model.addAttribute("grandTotal", grandTotal);

        return "order-success-bulk"; // We will create this HTML next
    }

    // ==========================================
    // 3. GENERATE BULK PDF INVOICE
    // ==========================================
    @GetMapping("/order/invoice/txn/{txnId}")
    public void downloadBulkInvoice(@PathVariable("txnId") String txnId, HttpServletResponse response) throws IOException {
        List<ProductOrder> orders = productOrderRepository.findByTransactionId(txnId);
        if (orders.isEmpty()) return;

        User user = orders.get(0).getUser();

        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Invoice_" + txnId + ".pdf";
        response.setHeader(headerKey, headerValue);

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());

        document.open();

        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
        Paragraph title = new Paragraph("INVOICE - MATHAPATI ELECTRICALS", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        
        document.add(new Paragraph("\n"));

        // Info
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        document.add(new Paragraph("Transaction ID: " + txnId, font));
        document.add(new Paragraph("Date: " + LocalDate.now(), font));
        document.add(new Paragraph("Customer: " + user.getFirstName() + " " + user.getLastName(), font));
        
        document.add(new Paragraph("\n"));

        // Table
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        
        // Header
        table.addCell(new Phrase("Product", font));
        table.addCell(new Phrase("Qty", font));
        table.addCell(new Phrase("Price (Each)", font));
        table.addCell(new Phrase("Total", font));

        double grandTotal = 0;

        // Loop through all items
        for (ProductOrder order : orders) {
            table.addCell(order.getProduct().getName());
            table.addCell(String.valueOf(order.getQuantity()));
            table.addCell("Rs. " + order.getProduct().getSellingPrice());
            table.addCell("Rs. " + order.getPrice());
            grandTotal += order.getPrice();
        }

        document.add(table);

        // Grand Total
        document.add(new Paragraph("\n"));
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Paragraph totalPara = new Paragraph("Grand Total: Rs. " + grandTotal, totalFont);
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        document.add(totalPara);

        document.close();
    }
}