package com.nabil.teranga_pay_api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        log.info("Authentication rejected: path={} reason={}",
                request.getRequestURI(), exception.getClass().getSimpleName());

        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        ProblemResponses.write(jsonMapper, response,
                HttpStatus.UNAUTHORIZED, "Authentification requise");
    }
}