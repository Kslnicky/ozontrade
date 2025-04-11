package me.yukitale.yellowexchange.security.auth.utils;

import eu.bitwalker.useragentutils.UserAgent;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ServletUtil {

    private final String[] VALID_IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" };

    public String getIpAddress(HttpServletRequest request) {
        for (String header : VALID_IP_HEADER_CANDIDATES) {
            String ipAddress = request.getHeader(header);
            if (ipAddress != null && ipAddress.length() != 0 && !"unknown".equalsIgnoreCase(ipAddress)) {
                return ipAddress;
            }
        }
        return request.getRemoteAddr();
    }

    public String getPlatform(HttpServletRequest request) {
        String platform = "N/A";
        try {
            UserAgent userAgent = UserAgent.parseUserAgentString(request.getHeader("user-agent"));
            platform = userAgent.getOperatingSystem().getName() + ", " + userAgent.getBrowser().getName();
        } catch (Exception ignored) {}
        return platform;
    }
}
