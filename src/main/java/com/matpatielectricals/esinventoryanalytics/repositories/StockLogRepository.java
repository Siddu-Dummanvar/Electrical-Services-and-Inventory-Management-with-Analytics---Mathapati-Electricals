package com.matpatielectricals.esinventoryanalytics.repositories;

import com.matpatielectricals.esinventoryanalytics.entities.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Long> {
    
    // ==========================================
    // 1. METHODS FOR SALES DASHBOARD (AdminController)
    // ==========================================
    
    // Fetch logs by type (RESTOCK or SALE) - Needed for Sales Report
    List<StockLog> findByActionOrderByTimestampDesc(String action);
    
    // Fetch all logs ordered by date - Needed for recent logs list
    List<StockLog> findAllByOrderByTimestampDesc();


    // ==========================================
    // 2. METHODS FOR INVENTORY ANALYTICS (InventoryController)
    // ==========================================

    // Get only the recent 20 logs 
    List<StockLog> findTop20ByOrderByTimestampDesc();

    // Calculate Total Expenses (Money spent on RESTOCK)
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM StockLog s WHERE s.action = 'RESTOCK'")
    Double getTotalExpenses();
    
    // Calculate Total Profit (Profit from SOLD items)
    @Query("SELECT COALESCE(SUM(s.profit), 0) FROM StockLog s WHERE s.action = 'SALE'")
    Double getTotalProfit();

    // Calculate Total Inventory Value (Current Stock * Cost Price)
    // This assumes your Product entity has a 'costPrice' field
    @Query("SELECT COALESCE(SUM(p.stock * p.costPrice), 0) FROM Product p")
    Double getCurrentInventoryValue();
}