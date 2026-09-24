package com.cinevora.security;

import com.cinevora.service.SensitiveActionRateLimiter;
import static com.cinevora.service.SensitiveActionRateLimiter.RateClass.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
public class AbuseProtectionFilter extends OncePerRequestFilter {
    private final SensitiveActionRateLimiter limiter;
    public AbuseProtectionFilter(SensitiveActionRateLimiter limiter) { this.limiter = limiter; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/v1/") || "OPTIONS".equals(request.getMethod())) { chain.doFilter(request, response); return; }
        if (request.getQueryString() != null && request.getQueryString().length() > 2048) { reject(response, 414, "URI too long"); return; }
        if (request.getContentLengthLong() > 65536 && (request.getContentType() == null || !request.getContentType().startsWith("multipart/"))) {
            reject(response, 413, "Request too large"); return;
        }
        var policy = path.endsWith("/login") ? AUTH_LOGIN_STRICT : path.endsWith("/register") ? AUTH_REGISTER
                : path.endsWith("/refresh") ? TOKEN_REFRESH : path.contains("password") ? AUTH_RESET_STRICT
                : path.startsWith("/api/v1/media/") ? UPLOAD
                : !"GET".equals(request.getMethod()) ? (path.contains("/admin/") ? ADMIN_WRITE : USER_WRITE)
                : path.contains("/home") || path.contains("recommendations") || path.contains("/users/me/") ? PERSONALIZED_EXPENSIVE
                : request.getParameter("q") != null ? PUBLIC_SEARCH : PUBLIC_READ;
        // Deliberately ignore untrusted Forwarded/X-Forwarded-For headers. Proxy peers share a budget
        // until the operator configures a verified trusted ingress topology.
        long retry = limiter.retryAfter(policy, "ip:" + request.getRemoteAddr());
        if (retry > 0) {
            response.setHeader("Retry-After", Long.toString(retry)); reject(response, 429, "Too many requests"); return;
        }
        if (request.getContentType() != null && request.getContentType().startsWith("application/json")) {
            byte[] body = request.getInputStream().readNBytes(65537);
            if (body.length > 65536) { reject(response, 413, "Request too large"); return; }
            chain.doFilter(new HttpServletRequestWrapper(request) {
                @Override public ServletInputStream getInputStream() {
                    var input = new java.io.ByteArrayInputStream(body);
                    return new ServletInputStream() {
                        public int read() { return input.read(); }
                        public boolean isFinished() { return input.available() == 0; }
                        public boolean isReady() { return true; }
                        public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
                    };
                }
                @Override public java.io.BufferedReader getReader() {
                    return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }, response);
        } else chain.doFilter(request, response);
    }
    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json"); response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}
