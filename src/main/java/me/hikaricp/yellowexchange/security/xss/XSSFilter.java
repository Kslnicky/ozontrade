package me.hikaricp.yellowexchange.security.xss;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import me.hikaricp.yellowexchange.security.xss.utils.XSSUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XSSFilter implements Filter {

    private static final List<String> EXCLUSION_FILTERS = Arrays.asList(
            "/api/admin/settings/email",
            "/api/admin/settings/presets",
            "/api/admin/settings/legals",
            "/api/admin/settings/errors",
            "/api/admin/telegram",
            "/api/admin/user-edit/errors",
            "/api/admin/user-edit/alert",
            "/api/supporter/settings/presets",
            "/api/supporter/user-edit/alert",
            "/api/supporter/user-edit/errors",
            "/api/worker/settings/legals",
            "/api/worker/settings/presets",
            "/api/worker/settings/errors",
            "/api/worker/user-edit/errors",
            "/api/worker/user-edit/alert",
            "/api/manager/user-edit/errors",
            "/api/manager/user-edit/alert"
    );

    private static boolean isExclusionUrl(String url) {
        return EXCLUSION_FILTERS.contains(url.toLowerCase().split("\\?")[0]);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (isExclusionUrl(((HttpServletRequest) request).getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String enctype = request.getContentType();
        if (StringUtils.isNotBlank(enctype) && enctype.toLowerCase().contains("multipart/form-data")) {
            chain.doFilter(request, response);
        } else {
            XSSRequestWrapper wrappedRequest = new XSSRequestWrapper ((HttpServletRequest) request);

            String body = IOUtils.toString(wrappedRequest.getReader());
            if (!body.isBlank()) {
                body = XSSUtils.stripXSS(body);
                wrappedRequest.resetInputStream(body.getBytes());
            }

            chain.doFilter(wrappedRequest, response);
        }
    }
}