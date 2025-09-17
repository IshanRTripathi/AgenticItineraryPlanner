package com.tripplanner.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.tripplanner.api.dto.ItineraryDto;
import com.tripplanner.data.repo.ItineraryRepository;
import com.tripplanner.security.GoogleUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

/**
 * Service for PDF generation.
 */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnBean(ItineraryRepository.class)
public class PdfService {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
    
    @Value("${pdf.base-url}")
    private String baseUrl;
    
    private final ItineraryRepository itineraryRepository;
    private final ItineraryService itineraryService;
    
    public PdfService(ItineraryRepository itineraryRepository, ItineraryService itineraryService) {
        this.itineraryRepository = itineraryRepository;
        this.itineraryService = itineraryService;
    }
    
    /**
     * Generate PDF for itinerary.
     */
    public byte[] generateItineraryPdf(String itineraryId, GoogleUserPrincipal user) {
        logger.info("Generating PDF for itinerary: {} for user: {}", itineraryId, user.getUserId());
        
        try {
            // Get itinerary data
            ItineraryDto itinerary = itineraryService.get(itineraryId, user);
            
            // Generate HTML content
            String htmlContent = generateHtmlContent(itinerary);
            
            // Convert HTML to PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(htmlContent, baseUrl);
            builder.toStream(outputStream);
            builder.run();
            
            byte[] pdfBytes = outputStream.toByteArray();
            
            logger.info("PDF generated successfully for itinerary: {}, size: {} bytes", itineraryId, pdfBytes.length);
            return pdfBytes;
            
        } catch (Exception e) {
            logger.error("Failed to generate PDF for itinerary: " + itineraryId, e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate HTML content for the itinerary.
     */
    private String generateHtmlContent(ItineraryDto itinerary) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<title>Itinerary - ").append(itinerary.destination()).append("</title>");
        html.append("<style>");
        html.append(getDefaultStyles());
        html.append("</style>");
        html.append("</head><body>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>Your Travel Itinerary</h1>");
        html.append("<h2>").append(itinerary.destination()).append("</h2>");
        html.append("<p class='dates'>");
        html.append(itinerary.startDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        html.append(" - ");
        html.append(itinerary.endDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        html.append("</p>");
        html.append("</div>");
        
        // Summary
        if (itinerary.summary() != null) {
            html.append("<div class='summary'>");
            html.append("<h3>Trip Summary</h3>");
            html.append("<p>").append(itinerary.summary()).append("</p>");
            html.append("</div>");
        }
        
        // Days
        if (itinerary.days() != null) {
            html.append("<div class='days'>");
            itinerary.days().forEach(day -> {
                html.append("<div class='day'>");
                html.append("<h3>Day ").append(day.day()).append(" - ").append(day.location()).append("</h3>");
                html.append("<p class='date'>").append(day.date()).append("</p>");
                
                if (day.activities() != null) {
                    html.append("<h4>Activities</h4>");
                    html.append("<ul>");
                    day.activities().forEach(activity -> {
                        html.append("<li>");
                        html.append("<strong>").append(activity.name()).append("</strong>");
                        if (activity.startTime() != null) {
                            html.append(" (").append(activity.startTime()).append(")");
                        }
                        if (activity.description() != null) {
                            html.append("<br><em>").append(activity.description()).append("</em>");
                        }
                        html.append("</li>");
                    });
                    html.append("</ul>");
                }
                
                html.append("</div>");
            });
            html.append("</div>");
        }
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>Generated by Agentic Itinerary Planner</p>");
        html.append("<p>Created on ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"))).append("</p>");
        html.append("</div>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Get default CSS styles for PDF.
     */
    private String getDefaultStyles() {
        return """
            body {
                font-family: Arial, sans-serif;
                line-height: 1.6;
                margin: 40px;
                color: #333;
            }
            .header {
                text-align: center;
                border-bottom: 2px solid #007bff;
                padding-bottom: 20px;
                margin-bottom: 30px;
            }
            .header h1 {
                color: #007bff;
                margin-bottom: 10px;
            }
            .header h2 {
                color: #666;
                margin-bottom: 10px;
            }
            .dates {
                font-size: 18px;
                color: #888;
            }
            .summary {
                background: #f8f9fa;
                padding: 20px;
                border-radius: 8px;
                margin-bottom: 30px;
            }
            .day {
                margin-bottom: 30px;
                page-break-inside: avoid;
            }
            .day h3 {
                color: #007bff;
                border-bottom: 1px solid #dee2e6;
                padding-bottom: 5px;
            }
            .day h4 {
                color: #495057;
                margin-top: 20px;
            }
            .day ul {
                margin: 10px 0;
            }
            .day li {
                margin-bottom: 10px;
            }
            .footer {
                margin-top: 50px;
                text-align: center;
                font-size: 12px;
                color: #888;
                border-top: 1px solid #dee2e6;
                padding-top: 20px;
            }
            """;
    }
}
