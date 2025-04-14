package me.hikaricp.yellowexchange.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public class CompositeLocaleResolver implements LocaleResolver {

    private final LocaleResolver userResolver;
    private final LocaleResolver adminResolver;

    public CompositeLocaleResolver(LocaleResolver userResolver, 
                                   LocaleResolver adminResolver) {
        this.userResolver = userResolver;
        this.adminResolver = adminResolver;
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/admin") || path.startsWith("/worker") || path.startsWith("/supporter") || path.startsWith("/api/admin") || path.startsWith("/api/worker") || path.startsWith("/api/supporter")) {
            return adminResolver.resolveLocale(request);
        } else {
            return userResolver.resolveLocale(request);
        }
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        String path = request.getRequestURI();
        if (path.startsWith("/admin") || path.startsWith("/worker") || path.startsWith("/supporter") || path.startsWith("/api/admin") || path.startsWith("/api/worker") || path.startsWith("/api/supporter")) {
            adminResolver.setLocale(request, response, locale);
        } else {
            userResolver.setLocale(request, response, locale);
        }
    }
}