package com.tripplanner.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to ensure proper UTF-8 encoding for all requests and responses
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class Utf8EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Only set encoding if not already set
        if (httpRequest.getCharacterEncoding() == null) {
            httpRequest.setCharacterEncoding("UTF-8");
        }
        
        // Only set response encoding for JSON content
        String contentType = httpResponse.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            if (!contentType.contains("charset")) {
                httpResponse.setContentType(contentType + "; charset=UTF-8");
            }
        }
        
        chain.doFilter(request, response);
    }
}
