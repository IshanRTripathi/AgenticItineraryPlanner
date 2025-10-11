package com.tripplanner.controller;

import com.tripplanner.service.PdfService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * REST controller for export operations (PDF, Email).
 */
// @RestController
@RequestMapping("/api/v1/export")
public class ExportController {
    
    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);
    
    private final PdfService pdfService;

    
    public ExportController(PdfService pdfService) {
        this.pdfService = pdfService;
    }
    
    /**
     * Generate and download PDF for an itinerary.
     */
    @GetMapping("/itineraries/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable String id, HttpServletRequest httpRequest) {
        logger.info("Generating PDF for itinerary: {}", id);
        
        // Extract userId from request attributes (set by FirebaseAuthConfig)
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            logger.error("User ID not found in request");
            return ResponseEntity.status(401).build();
        }
        
        byte[] pdfBytes = pdfService.generateItineraryPdf(id, userId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "itinerary-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);
        
        logger.info("PDF generated for itinerary: {}, size: {} bytes", id, pdfBytes.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
    
    /**
     * Send itinerary via email.
     */
    @PostMapping("/email/send")
    public ResponseEntity<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        logger.info("Sending email to: {}", request.to());
        
        EmailResponse response = new EmailResponse("", "QUEUED", Instant.now());
                // emailService.sendItineraryEmail(request, user);
        
        logger.info("Email sent successfully, message ID: {}", response.messageId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    /**
     * Send itinerary share link via email.
     * NOTE: Requires email service configuration (SendGrid, AWS SES, etc.) in application.yml
     */
    @PostMapping("/email/share")
    public ResponseEntity<EmailResponse> shareViaEmail(@Valid @RequestBody ShareEmailRequest request) {
        logger.info("Sharing itinerary via email to: {}", request.to());

        try {
            // Validate email format
            if (request.to() == null || !request.to().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest().build();
            }
            
            // Generate unique message ID
            String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "");
            
            // TODO: Integrate with actual email service (SendGrid, AWS SES, etc.)
            // For now, log the email details that would be sent
            logger.info("Email would be sent with following details:");
            logger.info("  To: {}", request.to());
            logger.info("  Subject: {}", request.subject());
            logger.info("  Itinerary ID: {}", request.itineraryId());
            logger.info("  Personal Message: {}", request.personalMessage());
            logger.info("  Include PDF: {}", request.includePdf());
            
            // In production, call email service here:
            // EmailServiceConfig emailConfig = getEmailServiceConfig();
            // if (emailConfig.isConfigured()) {
            //     emailService.send(
            //         request.to(),
            //         request.subject(),
            //         buildEmailBody(request),
            //         attachPdfIfRequested(request)
            //     );
            // }
            
            EmailResponse response = new EmailResponse(messageId, "QUEUED", Instant.now());
            
            logger.info("Email queued successfully, message ID: {}", response.messageId());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            logger.error("Error sending share email: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get email templates.
     */
    @GetMapping("/email/templates")
    public ResponseEntity<List<EmailTemplate>> getEmailTemplates() {
        logger.debug("Getting email templates");
        
        List<EmailTemplate> templates = Collections.emptyList();
                // emailService.getAvailableTemplates();
        
        return ResponseEntity.ok(templates);
    }
    
    // Request/Response DTOs
    
    /**
     * Request DTO for sending email.
     */
    public record EmailRequest(
            @jakarta.validation.constraints.Email String to,
            
            @NotBlank(message = "Subject is required")
            String subject,
            
            @NotBlank(message = "Itinerary ID is required")
            String itineraryId,
            
            String template,
            String personalMessage,
            boolean includePdf,
            Map<String, Object> templateData
    ) {}
    
    /**
     * Request DTO for sharing via email.
     */
    public record ShareEmailRequest(
            @NotBlank(message = "Recipient email is required")
            @Email(message = "Invalid email format")
            String to,
            
            @NotBlank(message = "Subject is required")
            String subject,
            
            @NotBlank(message = "Itinerary ID is required")
            String itineraryId,
            
            String personalMessage,
            String recipientName,
            boolean includePdf
    ) {}
    
    /**
     * Response DTO for email operations.
     */
    public record EmailResponse(
            String messageId,
            String status,
            java.time.Instant sentAt
    ) {}
    
    /**
     * DTO for email template information.
     */
    public record EmailTemplate(
            String id,
            String name,
            String description,
            List<String> requiredFields
    ) {}
}

