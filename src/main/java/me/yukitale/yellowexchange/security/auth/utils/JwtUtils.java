package me.yukitale.yellowexchange.security.auth.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import me.yukitale.yellowexchange.exchange.service.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

  private final Key jwtSecret = Keys.hmacShaKeyFor(new byte[] {13, 4, -48, 112, -2, 77, 29, 116, -24, 116, 122, 43, -2, -49, -5, 100, 88, -1, -127, 72, 89, -39, 54, -16, 34, -99, -107, -92, 72, 85, 78, -36, 66, 98, 111, -43, 30, 10, -103, 108, 20, 83, -41, -47, 118, 16, 22, -84, 124, -97, -105, -35, -83, -58, -2, -93, -49, -65, -88, -49, -85, -90, 12, 34});

  @Value("${yukitale.app.jwtExpirationMs}")
  private long jwtExpirationMs;

  @Value("${yukitale.app.jwtCookieName}")
  @Getter
  private String jwtCookie;

  public String getJwtFromCookies(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, jwtCookie);
    if (cookie != null) {
      return cookie.getValue();
    } else {
      return null;
    }
  }

  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) {
    String jwt = generateTokenFromEmail(userPrincipal.getUsername());
    return ResponseCookie.from(jwtCookie, jwt).path("/").maxAge(24 * 60 * 60).httpOnly(true).build();
  }

  public ResponseCookie getCleanJwtCookie() {
    return ResponseCookie.from(jwtCookie, null).path("/").build();
  }

  public String getEmailFromJwtToken(String token) {
    return extractClaims(token).getBody().getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      extractClaims(authToken);
      return true;
    } catch (Exception ignored) {}

    return false;
  }

  private Jws<Claims> extractClaims(String token) {
    return Jwts.parser().setSigningKey(jwtSecret).build().parseClaimsJws(token);
  }

  public String generateTokenFromEmail(String email) {
    return Jwts.builder()
            .setSubject(email)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(jwtSecret)
            .compact();
  }


  public String generateTokenFromEmailAndPassword(String email, String password) {
    return Jwts.builder()
            .setSubject(email + ";" + password)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(jwtSecret)
            .compact();
  }

  public Pair<String, String> getEmailAndPasswordFromJwtToken(String token) {
    String subject = extractClaims(token).getBody().getSubject();
    return Pair.of(subject.split(";")[0], subject.split(";")[1]);
  }
}
