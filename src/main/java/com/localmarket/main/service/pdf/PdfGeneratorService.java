package com.localmarket.main.service.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.localmarket.main.entity.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import java.util.Map;
import java.util.stream.Collectors;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;




@Service
@RequiredArgsConstructor
public class PdfGeneratorService {
    private final TemplateEngine templateEngine;

    public byte[] generateReceipt(Order order) {
        Context context = new Context();
        
        // Prepare data for template
        context.setVariable("orderId", order.getOrderId());
        context.setVariable("orderDate", order.getOrderDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
        context.setVariable("customerName", order.getCustomer() != null ? 
            order.getCustomer().getFirstname() + " " + order.getCustomer().getLastname() : "Guest Customer");
        context.setVariable("email", order.getCustomer() != null ? 
            order.getCustomer().getEmail() : order.getGuestEmail());
        context.setVariable("shippingAddress", order.getShippingAddress());
        
        // Calculate totals
        BigDecimal subtotal = order.getItems().stream()
            .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        context.setVariable("items", order.getItems().stream()
            .map(item -> Map.of(
                "name", item.getProduct().getName(),
                "quantity", item.getQuantity(),
                "price", item.getProduct().getPrice(),
                "total", item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
            ))
            .collect(Collectors.toList()));
            
        BigDecimal discount = subtotal.subtract(order.getTotalPrice());
        
        context.setVariable("subtotal", subtotal);
        context.setVariable("discount", discount);
        context.setVariable("total", order.getTotalPrice());

        // Process template
        String html = templateEngine.process("receipt-pdf", context);

        // Convert to PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        ConverterProperties properties = new ConverterProperties();
        HtmlConverter.convertToPdf(html, pdf, properties);
        return outputStream.toByteArray();
    }

}