package com.tripplanner.security;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Response wrapper to capture response data for idempotency caching.
 */
public class IdempotencyResponseWrapper extends HttpServletResponseWrapper {
    
    private final ByteArrayOutputStream capturedBody = new ByteArrayOutputStream();
    private final Map<String, String> capturedHeaders = new HashMap<>();
    private ServletOutputStream outputStream;
    private PrintWriter writer;
    
    public IdempotencyResponseWrapper(HttpServletResponse response) {
        super(response);
    }
    
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (outputStream == null) {
            outputStream = new CapturedServletOutputStream();
        }
        return outputStream;
    }
    
    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
        }
        return writer;
    }
    
    @Override
    public void setHeader(String name, String value) {
        super.setHeader(name, value);
        capturedHeaders.put(name, value);
    }
    
    @Override
    public void addHeader(String name, String value) {
        super.addHeader(name, value);
        capturedHeaders.put(name, value);
    }
    
    @Override
    public void setContentType(String type) {
        super.setContentType(type);
        capturedHeaders.put("Content-Type", type);
    }
    
    public String getCapturedBody() {
        try {
            if (writer != null) {
                writer.flush();
            }
            if (outputStream != null) {
                outputStream.flush();
            }
            return capturedBody.toString(getCharacterEncoding());
        } catch (IOException e) {
            return "";
        }
    }
    
    public Map<String, String> getCapturedHeaders() {
        return new HashMap<>(capturedHeaders);
    }
    
    /**
     * Custom ServletOutputStream that captures written data.
     */
    private class CapturedServletOutputStream extends ServletOutputStream {
        
        @Override
        public void write(int b) throws IOException {
            capturedBody.write(b);
            getResponse().getOutputStream().write(b);
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            capturedBody.write(b);
            getResponse().getOutputStream().write(b);
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            capturedBody.write(b, off, len);
            getResponse().getOutputStream().write(b, off, len);
        }
        
        @Override
        public boolean isReady() {
            return true;
        }
        
        @Override
        public void setWriteListener(WriteListener writeListener) {
            // Not implemented for this use case
        }
    }
}

