package com.yaswanth.Alumni_Bridge.config;

import com.yaswanth.Alumni_Bridge.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Allow public static resources and auth endpoints
        if (path.equals("/") || path.equals("/login") || path.equals("/register") || 
            path.equals("/logout") || path.equals("/error") || 
            path.startsWith("/uploads/") || path.startsWith("/css/") || 
            path.startsWith("/js/") || path.startsWith("/images/")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("loggedUser") : null;

        if (user == null) {
            String query = request.getQueryString();
            String target = (query != null && !query.isBlank()) ? path + "?" + query : path;
            String encodedTarget = URLEncoder.encode(target, StandardCharsets.UTF_8);
            response.sendRedirect("/login?redirect=" + encodedTarget + "&reason=auth_required");
            return false;
        }

        // Check if account has been blocked
        if (Boolean.TRUE.equals(user.getBlocked())) {
            session.invalidate();
            response.sendRedirect("/login?error=blocked");
            return false;
        }

        // Protect admin endpoints
        if (path.startsWith("/admin") && !"ADMIN".equals(user.getRole())) {
            response.sendRedirect("/dashboard");
            return false;
        }

        return true;
    }
}
