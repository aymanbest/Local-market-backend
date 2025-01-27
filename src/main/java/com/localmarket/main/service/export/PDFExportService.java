package com.localmarket.main.service.export;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.properties.UnitValue;
import java.time.format.DateTimeFormatter;
import com.localmarket.main.dto.analytics.CombinedAnalyticsResponse;
import java.io.ByteArrayOutputStream;
import com.itextpdf.layout.properties.HorizontalAlignment;

@Service
@RequiredArgsConstructor
public class PDFExportService {
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(48, 84, 150);
    private static final DeviceRgb SUBHEADER_COLOR = new DeviceRgb(91, 155, 213);
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm");

    public byte[] generatePDF(CombinedAnalyticsResponse analytics) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        try {
            // Company Header
            document.add(new Paragraph("LOCALMARKET")
                .setFontSize(15)
                .setBold()
                .setFontColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
            
            // Analytics Report Title
            document.add(new Paragraph("Analytics Report")
                .setFontSize(10)
                .setFontColor(SUBHEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
            
            // Period
            String formattedPeriod = String.format("Period: %s to %s",
                analytics.getPeriodStart().format(DATE_FORMATTER),
                analytics.getPeriodEnd().format(DATE_FORMATTER));
            
            document.add(new Paragraph(formattedPeriod)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));
            
            addSection(document, "User Analytics", analytics);
            addSection(document, "Transaction Analytics", analytics);
            addSection(document, "Business Metrics", analytics);
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
    
    private void addSection(Document document, String title, CombinedAnalyticsResponse analytics) {
        // Section Title with background
        Table headerTable = new Table(1).useAllAvailableWidth();
        Cell headerCell = new Cell()
            .add(new Paragraph(title))
            .setFontSize(16)
            .setBold()
            .setFontColor(ColorConstants.WHITE)
            .setBackgroundColor(HEADER_COLOR)
            .setPadding(5)
            .setTextAlignment(TextAlignment.CENTER);
        headerTable.addCell(headerCell);
        document.add(headerTable);
        
        // Data Table with fixed column widths
        float[] columnWidths = {150f, 100f};  // First column wider for labels
        Table table = new Table(UnitValue.createPointArray(columnWidths))
            .setMarginBottom(15)
            .setMarginTop(5)
            .setHorizontalAlignment(HorizontalAlignment.CENTER);
        
        // Add data with the same switch statement
        switch (title) {
            case "User Analytics":
                addTableRow(table, "Total Users", String.valueOf(analytics.getTotalUsers()));
                addTableRow(table, "Active Producers", String.valueOf(analytics.getActiveProducers()));
                addTableRow(table, "New Users", String.valueOf(analytics.getNewUsers()));
                addTableRow(table, "Active Producers %", String.format("%.2f%%", analytics.getActiveProducersPercentage()));
                addTableRow(table, "New Users %", String.format("%.2f%%", analytics.getNewUsersPercentage()));
                break;
                
            case "Transaction Analytics":
                addTableRow(table, "Total Volume", analytics.getTotalTransactionVolume().toString());
                addTableRow(table, "Completed Transactions", String.valueOf(analytics.getCompletedTransactions()));
                addTableRow(table, "Pending Transactions", String.valueOf(analytics.getPendingTransactions()));
                addTableRow(table, "Total Transactions", String.valueOf(analytics.getTotalTransactions()));
                break;
                
            case "Business Metrics":
                addTableRow(table, "Total Revenue", analytics.getTotalRevenue().toString());
                addTableRow(table, "Revenue Growth", String.format("%.2f%%", analytics.getRevenueGrowthRate()));
                addTableRow(table, "Active Users", String.valueOf(analytics.getActiveUsers()));
                addTableRow(table, "Active Users Growth", String.format("%.2f%%", analytics.getActiveUsersGrowthRate()));
                addTableRow(table, "Total Sales", String.valueOf(analytics.getTotalSales()));
                addTableRow(table, "Sales Growth", String.format("%.2f%%", analytics.getSalesGrowthRate()));
                addTableRow(table, "Overall Growth", String.format("%.2f%%", analytics.getOverallGrowthRate()));
                break;
        }
        
        document.add(table);
    }
    
    private void addTableRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
            .add(new Paragraph(label))
            .setBackgroundColor(new DeviceRgb(242, 242, 242))
            .setPadding(5)
            .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            
        Cell valueCell = new Cell()
            .add(new Paragraph(value))
            .setPadding(5)
            .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));
            
        table.addCell(labelCell);
        table.addCell(valueCell);
    }
} 