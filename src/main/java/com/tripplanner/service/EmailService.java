package com.tripplanner.service;

import com.tripplanner.api.ExportController;
import com.tripplanner.security.GoogleUserPrincipal;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for email operations.
 */
@Service
@ConditionalOnBean(JavaMailSender.class)
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Value("${email.smtp.from}")
    private String fromEmail;
    
    @Value("${email.smtp.from-name}")
    private String fromName;
    
    @Value("${app.frontend-url}")
    private String frontendUrl;
    
    private final JavaMailSender mailSender;
    private final PdfService pdfService;
    private final ItineraryService itineraryService;
    
    public EmailService(JavaMailSender mailSender, PdfService pdfService, ItineraryService itineraryService) {
        this.mailSender = mailSender;
        this.pdfService = pdfService;
        this.itineraryService = itineraryService;
    }
    
    /**
     * Send itinerary via email.
     */
    public ExportController.EmailResponse sendItineraryEmail(ExportController.EmailRequest request, GoogleUserPrincipal user) {
        logger.info("Sending itinerary email for user: {} to: {}", user.getUserId(), request.to());
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set email headers
            helper.setFrom(fromEmail, fromName);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            
            // Generate email content
            String htmlContent = generateItineraryEmailContent(request, user);
            helper.setText(htmlContent, true);
            
            // Attach PDF if requested
            if (request.includePdf()) {
                byte[] pdfBytes = pdfService.generateItineraryPdf(request.itineraryId(), user);
                helper.addAttachment("itinerary.pdf", new jakarta.mail.util.ByteArrayDataSource(pdfBytes, "application/pdf"));
            }
            
            // Send email
            mailSender.send(message);
            
            String messageId = UUID.randomUUID().toString();
            logger.info("Email sent successfully, message ID: {}", messageId);
            
            return new ExportController.EmailResponse(
                    messageId,
                    "sent",
                    Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to send itinerary email", e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Share itinerary via email.
     */
    public ExportController.EmailResponse shareItineraryViaEmail(ExportController.ShareEmailRequest request, GoogleUserPrincipal user) {
        logger.info("Sharing itinerary via email for user: {} to: {}", user.getUserId(), request.to());
        
        try {
            // Get itinerary and share it
            var shareResponse = itineraryService.share(request.itineraryId(), user);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            // Set email headers
            helper.setFrom(fromEmail, fromName);
            helper.setTo(request.to());
            helper.setSubject("Travel Itinerary Shared with You");
            
            // Generate share email content
            String htmlContent = generateShareEmailContent(request, shareResponse.publicUrl(), user);
            helper.setText(htmlContent, true);
            
            // Send email
            mailSender.send(message);
            
            String messageId = UUID.randomUUID().toString();
            logger.info("Share email sent successfully, message ID: {}", messageId);
            
            return new ExportController.EmailResponse(
                    messageId,
                    "sent",
                    Instant.now()
            );
            
        } catch (Exception e) {
            logger.error("Failed to send share email", e);
            throw new RuntimeException("Failed to send share email: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get available email templates.
     */
    public List<ExportController.EmailTemplate> getAvailableTemplates() {
        logger.debug("Getting available email templates");
        
        return Arrays.asList(
                new ExportController.EmailTemplate(
                        "itinerary",
                        "Itinerary Template",
                        "Standard template for sending complete itineraries",
                        Arrays.asList("itineraryId", "recipientName")
                ),
                new ExportController.EmailTemplate(
                        "share",
                        "Share Template",
                        "Template for sharing itineraries with others",
                        Arrays.asList("itineraryId", "senderName", "recipientName")
                ),
                new ExportController.EmailTemplate(
                        "booking-confirmation",
                        "Booking Confirmation",
                        "Template for booking confirmation emails",
                        Arrays.asList("bookingId", "confirmationNumber")
                )
        );
    }
    
    /**
     * Generate HTML content for itinerary email.
     */
    private String generateItineraryEmailContent(ExportController.EmailRequest request, GoogleUserPrincipal user) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Your Itinerary</title>");
        html.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px;}</style>");
        html.append("</head><body>");
        
        html.append("<h1 style='color:#007bff;'>Your Travel Itinerary</h1>");
        
        if (request.personalMessage() != null) {
            html.append("<div style='background:#f8f9fa;padding:15px;border-radius:5px;margin:20px 0;'>");
            html.append("<p><strong>Personal Message:</strong></p>");
            html.append("<p>").append(request.personalMessage()).append("</p>");
            html.append("</div>");
        }
        
        html.append("<p>Hi there!</p>");
        html.append("<p>Your travel itinerary has been generated and is ready for your review.</p>");
        
        if (request.includePdf()) {
            html.append("<p>Please find your complete itinerary attached as a PDF.</p>");
        }
        
        html.append("<p>You can also view your itinerary online at: ");
        html.append("<a href='").append(frontendUrl).append("/itinerary/").append(request.itineraryId()).append("'>");
        html.append("View Itinerary</a></p>");
        
        html.append("<p>Safe travels!</p>");
        html.append("<p>The Agentic Itinerary Planner Team</p>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
    
    /**
     * Generate HTML content for share email.
     */
    private String generateShareEmailContent(ExportController.ShareEmailRequest request, String publicUrl, GoogleUserPrincipal user) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head><meta charset='UTF-8'><title>Shared Itinerary</title>");
        html.append("<style>body{font-family:Arial,sans-serif;line-height:1.6;color:#333;max-width:600px;margin:0 auto;padding:20px;}</style>");
        html.append("</head><body>");
        
        html.append("<h1 style='color:#007bff;'>Travel Itinerary Shared with You</h1>");
        
        String recipientName = request.recipientName() != null ? request.recipientName() : "there";
        html.append("<p>Hi ").append(recipientName).append("!</p>");
        
        html.append("<p>").append(user.getDisplayName()).append(" has shared a travel itinerary with you.</p>");
        
        if (request.personalMessage() != null) {
            html.append("<div style='background:#f8f9fa;padding:15px;border-radius:5px;margin:20px 0;'>");
            html.append("<p><strong>Personal Message:</strong></p>");
            html.append("<p>").append(request.personalMessage()).append("</p>");
            html.append("</div>");
        }
        
        html.append("<p><a href='").append(frontendUrl).append(publicUrl).append("' ");
        html.append("style='background:#007bff;color:white;padding:12px 24px;text-decoration:none;border-radius:5px;display:inline-block;'>");
        html.append("View Itinerary</a></p>");
        
        html.append("<p>This itinerary was created using Agentic Itinerary Planner.</p>");
        
        html.append("</body></html>");
        
        return html.toString();
    }
}
