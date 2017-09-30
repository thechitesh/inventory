package com.inventory.config;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

@Component
public class InventoryFilter extends GenericFilterBean {

  private final AuthenticationManager authenticationManager;

  @Autowired
  public InventoryFilter(final AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @Override
  public final void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) {

    try {
      final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

      final InventoryUserAuthentication token;
      token = getTokenDetails(httpServletRequest.getHeader("Authorization"));

      final Authentication auth =
          authenticationManager.authenticate(token);
      SecurityContextHolder.getContext().setAuthentication(auth);
      chain.doFilter(request, response);

    } catch (ParseException | IOException | ServletException e) {
      e.printStackTrace();
      try {
        dispatchResponse(response, HttpStatus.UNAUTHORIZED.value(), e);
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private InventoryUserAuthentication getTokenDetails(final String token) throws ParseException {
    final JWT jwt = JWTParser.parse(token);
    final JWTClaimsSet jwtClaimsSet = jwt.getJWTClaimsSet();
    final String role = "ROLE_" + jwtClaimsSet.getStringClaim("role");
    final String username = jwtClaimsSet.getStringClaim("username");
    final String userid = jwtClaimsSet.getStringClaim("userid");
    final InventoryUserAuthentication inventoryUserAuthentication = new InventoryUserAuthentication(
        username,
        userid,
        Collections.singletonList(new SimpleGrantedAuthority(role)),
        (SignedJWT) jwt
    );
    inventoryUserAuthentication.setAuthenticated(false);
    return inventoryUserAuthentication;
  }


  private void dispatchResponse(final ServletResponse response,
                                final Integer statusCode,
                                final Exception e) throws IOException {
    logger.error(e);
    ((HttpServletResponse) response).setStatus(statusCode);
    response.getWriter().append("Ayuthentication failed");
  }
}
