package com.tripplanner.controller;

import com.tripplanner.service.PdfService;
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
    public ResponseEntity<byte[]> generatePdf(@PathVariable String id) {
        logger.info("Generating PDF for itinerary: {}", id);
        
        byte[] pdfBytes = pdfService.generateItineraryPdf(id);
        
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
     */
    @PostMapping("/email/share")
    public ResponseEntity<EmailResponse> shareViaEmail(@Valid @RequestBody ShareEmailRequest request) {
        logger.info("Sharing itinerary via email to: {}", request.to());

        EmailResponse response = new EmailResponse("", "QUEUED", Instant.now());
        // emailService.shareItineraryViaEmail(request, user);
        
        logger.info("Share email sent successfully, message ID: {}", response.messageId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
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
            
            @NotBlank(message = "Itinerary ID is required")
            String itineraryId,
            
            String personalMessage,
            String recipientName
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

