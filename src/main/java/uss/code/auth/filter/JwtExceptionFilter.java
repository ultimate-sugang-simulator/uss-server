package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import uss.code.global.exception.domain.ExceptionCode;
import uss.code.global.exception.domain.JwtAuthenticationException;
import uss.code.global.exception.domain.RestApiException;
import uss.code.global.exception.dto.response.ErrorResponse;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        try{
            filterChain.doFilter(request, response);
        }catch (final JwtAuthenticationException e){
            setErrorResponse(response, SC_UNAUTHORIZED, e.getCode(), e.getMessage());
        }catch (final RestApiException e){
            final ExceptionCode exceptionCode = e.getExceptionCode();
            setErrorResponse(response, exceptionCode.getStatus().value(), exceptionCode.getCode(), exceptionCode.getMessage());
        }
    }

    private void setErrorResponse(
            final HttpServletResponse response,
            final int status,
            final int code,
            final String message
    )throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);

        ErrorResponse errorResponse = ErrorResponse.of(code, message);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
