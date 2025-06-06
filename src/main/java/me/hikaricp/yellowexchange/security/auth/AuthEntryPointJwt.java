package me.hikaricp.yellowexchange.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
    if (authException instanceof BadCredentialsException) {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

      Map<String, Object> body = new HashMap<>();
      body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
      body.put("error", "Unauthorized");
      body.put("message", authException.getMessage());
      body.put("path", request.getServletPath());

      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(response.getOutputStream(), body);
      return;
    }

    response.sendRedirect("/signin");
  }
}
