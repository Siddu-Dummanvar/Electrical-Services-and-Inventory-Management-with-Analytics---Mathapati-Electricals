package com.matpatielectricals.esinventoryanalytics.controller;

import com.matpatielectricals.esinventoryanalytics.entities.Product;
import com.matpatielectricals.esinventoryanalytics.entities.StockLog;
import com.matpatielectricals.esinventoryanalytics.repositories.ProductRepository;
import com.matpatielectricals.esinventoryanalytics.repositories.StockLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/inventory")
public class InventoryController {

    @Autowired private ProductRepository productRepository;
    @Autowired private StockLogRepository stockLogRepository;

    // --- 1. SHOW ANALYTICS DASHBOARD ---
    @GetMapping("/analytics")
    public String showInventoryAnalytics(Model model) {
        
        // A. Financial Cards
        Double totalExpense = stockLogRepository.getTotalExpenses();
        Double totalProfit = stockLogRepository.getTotalProfit();
        Double inventoryValue = stockLogRepository.getCurrentInventoryValue();
        
        model.addAttribute("totalExpense", totalExpense);
        model.addAttribute("totalProfit", totalProfit);
        model.addAttribute("inventoryValue", inventoryValue);

        // B. Low Stock Alert (< 10 items)
        List<Product> lowStockItems = productRepository.findAll().stream()
                .filter(p -> p.getStock() < 10)
                .toList();
        model.addAttribute("lowStockItems", lowStockItems);

        // C. Recent Logs
        model.addAttribute("logs", stockLogRepository.findTop20ByOrderByTimestampDesc());

        return "inventory-analytics"; // Frontend HTML
    }

    // --- 2. RESTOCK ACTION (Admin Buys Stock) ---
    @PostMapping("/restock")
    public String addStock(@RequestParam("productId") Long productId,
                           @RequestParam("quantity") int quantity,
                           @RequestParam("costPrice") double costPrice) {
        
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            // 1. Update Product Stock & Cost Price
            product.setStock(product.getStock() + quantity);
            product.setCostPrice(costPrice); // Update latest cost price
            productRepository.save(product);

            // 2. Log the Expense
            StockLog log = new StockLog();
            log.setProduct(product);
            log.setAction("RESTOCK");
            log.setQuantity(quantity);
            log.setUnitPrice(costPrice);
            log.setTotalAmount(quantity * costPrice); // EXPENSE
            log.setTimestamp(LocalDateTime.now());
            stockLogRepository.save(log);
        }
        return "redirect:/admin/inventory/analytics";
    }
}