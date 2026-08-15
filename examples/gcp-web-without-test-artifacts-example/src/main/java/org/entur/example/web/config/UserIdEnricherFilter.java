package org.entur.example.web.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

// for testing
public class UserIdEnricherFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // emulate a filter enriching the request
        MDC.put("subject", "my-subject-id");
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("subject");
        }
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}