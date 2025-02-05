package me.yukitale.yellowexchange.security.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.yukitale.yellowexchange.security.auth.utils.JwtUtils;
import me.yukitale.yellowexchange.exchange.repository.user.EmailBanRepository;
import me.yukitale.yellowexchange.exchange.service.UserDetailsServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthTokenFilter.class);

  private static final String[] PROTECTED_PATHS = {
          "/", "/signin", "/signup", "/signin-2fa", "/forgot-password", "/email",
          "/api/worker/**", "/api/admin/**", "/api/supporter/**",
          "/api/user/**", "/profile/**", "/worker/**",
          "/admin/**", "/supporter/**"
  };

  @Autowired
  private EmailBanRepository emailBanRepository;

  @Autowired
  private JwtUtils jwtUtils;

  @Autowired
  private UserDetailsServiceImpl userDetailsService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    try {
      String requestURI = request.getRequestURI().toLowerCase();

      boolean authorized = false;
      if (requiresAuthentication(requestURI)) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
          String email = jwtUtils.getEmailFromJwtToken(jwt);
          if (emailBanRepository.existsByEmail(email)) {
            response.sendRedirect("/banned");
            return;
          }

          UserDetails userDetails = userDetailsService.loadUserByUsername(email);

          if (userDetails != null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            authorized = true;
          }
        }
      }

      if (!authorized && (requestURI.equals("/") || requestURI.startsWith("/signin") || requestURI.startsWith("/signup"))) {
        String referrer = request.getHeader("referrer");
        if (StringUtils.isBlank(referrer)) {
          referrer = request.getHeader("referer");
        }
        if (StringUtils.isNotBlank(referrer)) {
          referrer = referrer.replace("http://", "").replace("https://", "").toLowerCase();
          if (referrer.startsWith("www.")) {
            referrer = referrer.substring(4);
          }

          if (referrer.contains("/")) {
            referrer = referrer.split("/")[0];
          }

          referrer = referrer.toLowerCase();

          String host = request.getHeader("host").toLowerCase();
          if (!referrer.equals(host)) {
            response.addCookie(new Cookie("referrer", referrer));
          }
        }
      }
    } catch (Exception e) {
      LOGGER.error("Cannot set user authentication: {}", e);
    }

    filterChain.doFilter(request, response);
  }

  private boolean requiresAuthentication(String requestURI) {
    for (String path : PROTECTED_PATHS) {
      if (path.endsWith("/**")) {
        String basePath = path.substring(0, path.length() - 3);
        if (requestURI.startsWith(basePath)) {
          return true;
        }
      } else {
        if (requestURI.equals(path)) {
          return true;
        }
      }
    }

    return false;
  }

  private String parseJwt(HttpServletRequest request) {
    return jwtUtils.getJwtFromCookies(request);
  }
}
