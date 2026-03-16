package com.matpatielectricals.esinventoryanalytics.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.matpatielectricals.esinventoryanalytics.entities.ServiceRequest;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;


@Controller
public class PaymentController {

    @PostMapping("/create_order")
    @ResponseBody
    public String createOrder(@RequestBody Map<String, Object> data) throws Exception {
        
        // 1. Get Amount (Frontend sends it in Rupees)
        int amount = Integer.parseInt(data.get("amount").toString());

        // 2. Initialize Razorpay Client
        // GO TO razorpay.com -> Log in -> Settings -> API Keys -> Generate Key
        // PASTE THEM HERE
        var client = new RazorpayClient("rzp_test_RnwFYefaZom83T", "kceYtoAEV6OxFKsP93d6ARHM");

        // 3. Create Order Request
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // Convert to Paise (Required by Razorpay)
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_123456");

        // 4. Create Order
        Order order = client.orders.create(orderRequest);

        // 5. Return Order Details to Frontend
        return order.toString();
    }
    
    @PostMapping("/save_order")
    @ResponseBody
    public String saveOrder(@RequestBody Map<String, Object> data) {
        
        String mode = (String) data.get("paymentMode");
        String status = (String) data.get("paymentStatus");
        String txnId = (String) data.get("transactionId");
        
        System.out.println("Order Mode: " + mode);
        System.out.println("Payment Status: " + status);
        
        // TODO: Write your Database Logic here to save the order
        // orderRepo.save(new Order(...));
        
        return "success";
    }
    
    @Autowired
    private com.matpatielectricals.esinventoryanalytics.repositories.ServiceRequestRepository serviceRequestRepository;
    
    @Autowired
    private com.matpatielectricals.esinventoryanalytics.repositories.UserRepository userRepository;

    @PostMapping("/save_service_request")
    @ResponseBody
    public String saveServiceRequest(@RequestBody Map<String, Object> data, jakarta.servlet.http.HttpSession session) {
        
        // 1. Check Login
        com.matpatielectricals.esinventoryanalytics.entities.User sessionUser = 
            (com.matpatielectricals.esinventoryanalytics.entities.User) session.getAttribute("sessionUser");
            
        if (sessionUser == null) {
            return "redirect:/user-Login";
        }

        try {
            // 2. Parse Data from Frontend
            ServiceRequest req = new ServiceRequest();
            req.setUser(sessionUser); // Link to logged in user
            
            req.setFullName((String) data.get("fullName"));
            req.setServiceType((String) data.get("serviceType"));
            req.setLocation((String) data.get("location"));
            req.setPhoneNumber((String) data.get("phonenumber"));
            req.setAltPhoneNumber((String) data.get("altphonenumber"));
            req.setEmail((String) data.get("email"));
            req.setDetails((String) data.get("details"));
            
            // Handle Date & Time
            req.setPreferredDate(LocalDate.parse((String) data.get("serviceDate")));
            req.setPreferredTime(LocalTime.parse((String) data.get("serviceTime")));
            
            // Payment Info
            req.setAmount(Double.parseDouble(data.get("amount").toString()));
            req.setTransactionId((String) data.get("transactionId"));
            req.setPaymentStatus("Success");
            
            // Initial Admin Status
            req.setServiceStatus("New Request"); 
            
            // 3. Save to DB
            serviceRequestRepository.save(req);
            
            return "saved";
            
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }
}
