package com.matpatielectricals.esinventoryanalytics.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.matpatielectricals.esinventoryanalytics.entities.*;
import com.matpatielectricals.esinventoryanalytics.repositories.*;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// ✅ Import specifically for Excel Row to avoid conflict
import org.apache.poi.ss.usermodel.Row; 

@Service
public class ReportService {

    @Autowired private ProductOrderRepository orderRepo;
    @Autowired private ServiceRequestRepository serviceRepo;
    @Autowired private StockLogRepository stockRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private EmployeeRepository empRepo;

    // ==========================================
    // 1. GENERATE PDF REPORT
    // ==========================================
    public void generateDailyPdf(LocalDate date, HttpServletResponse response) throws IOException {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // Fonts
        com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLUE);
        com.lowagie.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
        com.lowagie.text.Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        // Date Range (Start to End of Day)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // --- FETCH DATA ---
        List<ProductOrder> orders = orderRepo.findByOrderDate(date);
        List<ServiceRequest> services = serviceRepo.findByCompletionDate(date);
        List<StockLog> logs = stockRepo.findByTimestampBetween(startOfDay, endOfDay);
        
        // Filter for specific lists
        List<StockLog> restocks = logs.stream().filter(l -> "RESTOCK".equalsIgnoreCase(l.getAction())).toList();
        List<ProductOrder> returns = orders.stream().filter(o -> o.getReturnStatus() != null).toList();
        
        List<User> newCustomers = userRepo.findByRegistrationDateBetween(startOfDay, endOfDay);
        List<Employee> newEmployees = empRepo.findByRegistrationDateBetween(startOfDay, endOfDay);

        // --- TITLE ---
        Paragraph title = new Paragraph("Daily Business Report: " + date, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph("\n"));

        // --- 1. FINANCIAL SUMMARY ---
        PdfPTable finTable = new PdfPTable(2);
        finTable.setWidthPercentage(100);
        finTable.setSpacingBefore(10f); // ✅ ADDED SPACE BEFORE TABLE
        finTable.setSpacingAfter(10f);  // ✅ ADDED SPACE AFTER TABLE
        
        addPdfHeader(finTable, "Metric", headerFont);
        addPdfHeader(finTable, "Value", headerFont);

        double prodRev = orders.stream().filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).mapToDouble(ProductOrder::getPrice).sum();
        double servRev = services.stream().mapToDouble(s -> s.getAmount() != null ? s.getAmount() : 0.0).sum();
        double dailyExpense = restocks.stream().mapToDouble(StockLog::getTotalAmount).sum();

        finTable.addCell(new Phrase("Product Sales Revenue", dataFont));
        finTable.addCell(new Phrase("Rs. " + prodRev, dataFont));
        finTable.addCell(new Phrase("Service Revenue", dataFont));
        finTable.addCell(new Phrase("Rs. " + servRev, dataFont));
        finTable.addCell(new Phrase("Restock Expenses", dataFont));
        finTable.addCell(new Phrase("Rs. " + dailyExpense, dataFont));
        
        PdfPCell profitCell = new PdfPCell(new Phrase("NET PROFIT", headerFont));
        profitCell.setBackgroundColor(new Color(34, 139, 34));
        finTable.addCell(profitCell);
        finTable.addCell(new Phrase("Rs. " + ((prodRev + servRev) - dailyExpense), dataFont));

        document.add(new Paragraph("1. Financial Overview", subHeaderFont));
        document.add(finTable);
        // Removed explicit new Paragraph("\n") because setSpacingAfter handles it

        // --- 2. NEW REGISTRATIONS (Customers & Employees) ---
        document.add(new Paragraph("2. New Registrations (" + newCustomers.size() + " Users, " + newEmployees.size() + " Staff)", subHeaderFont));
        
        if (!newCustomers.isEmpty()) {
            document.add(new Paragraph("New Customers:", dataFont));
            PdfPTable userTable = new PdfPTable(3);
            userTable.setWidthPercentage(100);
            userTable.setSpacingBefore(5f); // ✅ Space between "New Customers:" text and table
            userTable.setSpacingAfter(10f);
            
            addPdfHeader(userTable, "Name", headerFont);
            addPdfHeader(userTable, "Email", headerFont);
            addPdfHeader(userTable, "City", headerFont);
            for(User u : newCustomers) {
                userTable.addCell(new Phrase(u.getFirstName() + " " + u.getLastName(), dataFont));
                userTable.addCell(new Phrase(u.getEmailid(), dataFont));
                userTable.addCell(new Phrase(u.getCity(), dataFont));
            }
            document.add(userTable);
        }
        
        if (!newEmployees.isEmpty()) {
            document.add(new Paragraph("New Employees (Approved/Added):", dataFont));
            PdfPTable empTable = new PdfPTable(3);
            empTable.setWidthPercentage(100);
            empTable.setSpacingBefore(5f); // ✅ Space between text and table
            empTable.setSpacingAfter(10f);
            
            addPdfHeader(empTable, "Name", headerFont);
            addPdfHeader(empTable, "Job Title", headerFont);
            addPdfHeader(empTable, "Status", headerFont);
            for(Employee e : newEmployees) {
                empTable.addCell(new Phrase(e.getFirstName() + " " + e.getLastName(), dataFont));
                empTable.addCell(new Phrase(e.getJobTitle(), dataFont));
                empTable.addCell(new Phrase(e.getStatus(), dataFont));
            }
            document.add(empTable);
        }
        
        // Add a spacer if both tables were empty to separate sections
        if (newCustomers.isEmpty() && newEmployees.isEmpty()) {
             document.add(new Paragraph("\n"));
        }

        // --- 3. RESTOCK ACTIVITY ---
        document.add(new Paragraph("3. Products Restocked Today", subHeaderFont));
        if (restocks.isEmpty()) {
            document.add(new Paragraph("No restock activity today.", dataFont));
            document.add(new Paragraph("\n"));
        } else {
            PdfPTable stockTable = new PdfPTable(4);
            stockTable.setWidthPercentage(100);
            stockTable.setSpacingBefore(10f); // ✅ ADDED SPACE
            stockTable.setSpacingAfter(10f);
            
            addPdfHeader(stockTable, "Product", headerFont);
            addPdfHeader(stockTable, "Qty Added", headerFont);
            addPdfHeader(stockTable, "Unit Cost", headerFont);
            addPdfHeader(stockTable, "Total Cost", headerFont);
            
            for(StockLog log : restocks) {
                stockTable.addCell(new Phrase(log.getProduct().getName(), dataFont));
                stockTable.addCell(new Phrase(String.valueOf(log.getQuantity()), dataFont));
                stockTable.addCell(new Phrase("Rs. " + log.getUnitPrice(), dataFont));
                stockTable.addCell(new Phrase("Rs. " + log.getTotalAmount(), dataFont));
            }
            document.add(stockTable);
        }

        // --- 4. RETURNED PRODUCTS ---
        document.add(new Paragraph("4. Product Returns (From Orders Placed Today)", subHeaderFont));
        if (returns.isEmpty()) {
            document.add(new Paragraph("No returns requested for today's orders.", dataFont));
        } else {
            PdfPTable returnTable = new PdfPTable(3);
            returnTable.setWidthPercentage(100);
            returnTable.setSpacingBefore(10f); // ✅ ADDED SPACE
            returnTable.setSpacingAfter(10f);
            
            addPdfHeader(returnTable, "Order ID", headerFont);
            addPdfHeader(returnTable, "Product", headerFont);
            addPdfHeader(returnTable, "Return Status", headerFont);
            
            for(ProductOrder o : returns) {
                returnTable.addCell(new Phrase("#" + o.getId(), dataFont));
                returnTable.addCell(new Phrase(o.getProduct().getName(), dataFont));
                returnTable.addCell(new Phrase(o.getReturnStatus(), dataFont));
            }
            document.add(returnTable);
        }

        document.close();
    }

    private void addPdfHeader(PdfPTable table, String text, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.DARK_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8); // ✅ Increased Padding slightly for better look
        table.addCell(cell);
    }

    // ==========================================
    // 2. GENERATE EXCEL REPORT
    // ==========================================
    public void generateDailyExcel(LocalDate date, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Daily Report " + date);

        CellStyle headerStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        int rowNum = 0;
        
        // Data Fetching
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        List<ProductOrder> orders = orderRepo.findByOrderDate(date);
        List<ServiceRequest> services = serviceRepo.findByCompletionDate(date);
        List<StockLog> logs = stockRepo.findByTimestampBetween(startOfDay, endOfDay);
        List<StockLog> restocks = logs.stream().filter(l -> "RESTOCK".equalsIgnoreCase(l.getAction())).toList();
        List<User> newCustomers = userRepo.findByRegistrationDateBetween(startOfDay, endOfDay);
        List<Employee> newEmployees = empRepo.findByRegistrationDateBetween(startOfDay, endOfDay);
        List<ProductOrder> returns = orders.stream().filter(o -> o.getReturnStatus() != null).toList();

        // TITLE
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue("Daily Report: " + date);

        // --- FINANCIALS ---
        rowNum++;
        Row finHeader = sheet.createRow(rowNum++);
        finHeader.createCell(0).setCellValue("FINANCIAL SUMMARY");
        finHeader.getCell(0).setCellStyle(headerStyle);

        double prodRev = orders.stream().filter(o -> "Paid".equalsIgnoreCase(o.getStatus())).mapToDouble(ProductOrder::getPrice).sum();
        double servRev = services.stream().mapToDouble(s -> s.getAmount() != null ? s.getAmount() : 0.0).sum();
        double dailyExpense = restocks.stream().mapToDouble(StockLog::getTotalAmount).sum();

        sheet.createRow(rowNum++).createCell(0).setCellValue("Sales Rev: " + prodRev);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Service Rev: " + servRev);
        sheet.createRow(rowNum++).createCell(0).setCellValue("Expenses: " + dailyExpense);
        sheet.createRow(rowNum++).createCell(0).setCellValue("NET PROFIT: " + ((prodRev + servRev) - dailyExpense));

        // --- NEW CUSTOMERS ---
        rowNum += 2;
        Row custHeader = sheet.createRow(rowNum++);
        custHeader.createCell(0).setCellValue("NEW CUSTOMERS (" + newCustomers.size() + ")");
        custHeader.getCell(0).setCellStyle(headerStyle);
        if(!newCustomers.isEmpty()){
            Row h = sheet.createRow(rowNum++);
            h.createCell(0).setCellValue("Name"); h.createCell(1).setCellValue("Email"); h.createCell(2).setCellValue("City");
            for(User u : newCustomers) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(u.getFirstName() + " " + u.getLastName());
                r.createCell(1).setCellValue(u.getEmailid());
                r.createCell(2).setCellValue(u.getCity());
            }
        }

        // --- NEW EMPLOYEES ---
        rowNum += 2;
        Row empHeader = sheet.createRow(rowNum++);
        empHeader.createCell(0).setCellValue("NEW EMPLOYEES (" + newEmployees.size() + ")");
        empHeader.getCell(0).setCellStyle(headerStyle);
        if(!newEmployees.isEmpty()){
            Row h = sheet.createRow(rowNum++);
            h.createCell(0).setCellValue("Name"); h.createCell(1).setCellValue("Role"); h.createCell(2).setCellValue("Status");
            for(Employee e : newEmployees) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(e.getFirstName() + " " + e.getLastName());
                r.createCell(1).setCellValue(e.getJobTitle());
                r.createCell(2).setCellValue(e.getStatus());
            }
        }

        // --- RESTOCK ---
        rowNum += 2;
        Row stockHeader = sheet.createRow(rowNum++);
        stockHeader.createCell(0).setCellValue("RESTOCK ACTIVITY");
        stockHeader.getCell(0).setCellStyle(headerStyle);
        if(!restocks.isEmpty()){
            Row h = sheet.createRow(rowNum++);
            h.createCell(0).setCellValue("Product"); h.createCell(1).setCellValue("Qty"); h.createCell(2).setCellValue("Total Cost");
            for(StockLog s : restocks) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(s.getProduct().getName());
                r.createCell(1).setCellValue(s.getQuantity());
                r.createCell(2).setCellValue(s.getTotalAmount());
            }
        }

        // --- RETURNS ---
        rowNum += 2;
        Row retHeader = sheet.createRow(rowNum++);
        retHeader.createCell(0).setCellValue("RETURNS (Today's Orders)");
        retHeader.getCell(0).setCellStyle(headerStyle);
        if(!returns.isEmpty()){
            Row h = sheet.createRow(rowNum++);
            h.createCell(0).setCellValue("Order ID"); h.createCell(1).setCellValue("Product"); h.createCell(2).setCellValue("Status");
            for(ProductOrder o : returns) {
                Row r = sheet.createRow(rowNum++);
                r.createCell(0).setCellValue(o.getId());
                r.createCell(1).setCellValue(o.getProduct().getName());
                r.createCell(2).setCellValue(o.getReturnStatus());
            }
        }

        // Auto Size
        for(int i=0; i<4; i++) sheet.autoSizeColumn(i);

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}