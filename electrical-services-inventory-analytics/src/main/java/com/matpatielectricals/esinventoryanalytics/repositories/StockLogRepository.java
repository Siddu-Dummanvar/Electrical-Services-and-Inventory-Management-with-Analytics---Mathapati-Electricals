package com.matpatielectricals.esinventoryanalytics.repositories;

import com.matpatielectricals.esinventoryanalytics.entities.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDateTime; // ✅ ADDED THIS IMPORT

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Long> {
    
    // Fetch logs by type (RESTOCK or SALE)
    List<StockLog> findByActionOrderByTimestampDesc(String action);
    
    // Fetch all logs ordered by date
    List<StockLog> findAllByOrderByTimestampDesc();

    // Get only the recent 20 logs 
    List<StockLog> findTop20ByOrderByTimestampDesc();

    // Calculate Total Expenses
    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM StockLog s WHERE s.action = 'RESTOCK'")
    Double getTotalExpenses();
    
    // Calculate Total Profit
    @Query("SELECT COALESCE(SUM(s.profit), 0) FROM StockLog s WHERE s.action = 'SALE'")
    Double getTotalProfit();

    // Calculate Total Inventory Value
    @Query("SELECT COALESCE(SUM(p.stock * p.costPrice), 0) FROM Product p")
    Double getCurrentInventoryValue();

    // ✅ ADDED THIS METHOD (Fixes your error)
    List<StockLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}