package com.localmarket.main.service.export;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.io.StringWriter;

import com.localmarket.main.dto.analytics.admin.CombinedAnalyticsResponse;
import com.opencsv.CSVWriter;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class CSVExportService {
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm");

    public byte[] generateCSV(CombinedAnalyticsResponse analytics) {
        StringWriter writer = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(writer)) {
            // Company Header
            csvWriter.writeNext(new String[] {"LOCALMARKET"});
            csvWriter.writeNext(new String[] {"Analytics Report"});
            csvWriter.writeNext(new String[] {"Period: " + 
                analytics.getPeriodStart().format(DATE_FORMATTER) + " to " + 
                analytics.getPeriodEnd().format(DATE_FORMATTER)});
            csvWriter.writeNext(new String[] {});
            
            // User Analytics Section
            csvWriter.writeNext(new String[] {"User Analytics"});
            csvWriter.writeNext(new String[] {"Total Users", String.valueOf(analytics.getTotalUsers())});
            csvWriter.writeNext(new String[] {"Active Producers", String.valueOf(analytics.getActiveProducers())});
            csvWriter.writeNext(new String[] {"New Users", String.valueOf(analytics.getNewUsers())});
            csvWriter.writeNext(new String[] {});
            
            // Transaction Analytics Section
            csvWriter.writeNext(new String[] {"Transaction Analytics"});
            csvWriter.writeNext(new String[] {"Total Transactions", String.valueOf(analytics.getTotalTransactions())});
            csvWriter.writeNext(new String[] {"Average Order Value", analytics.getAverageOrderValue().toString()});
            csvWriter.writeNext(new String[] {});
            
            // Business Metrics Section
            csvWriter.writeNext(new String[] {"Business Metrics"});
            csvWriter.writeNext(new String[] {"Total Revenue", analytics.getTotalRevenue().toString()});
            csvWriter.writeNext(new String[] {"Revenue Growth", 
                String.format("%.2f%%", analytics.getRevenueGrowthRate())});
            
            return writer.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV", e);
        }
    }
} 