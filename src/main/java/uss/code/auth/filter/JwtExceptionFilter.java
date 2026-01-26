package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import uss.code.global.exception.domain.JwtAuthenticationException;
import uss.code.global.exception.dto.response.ErrorResponse;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.*;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

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
        }catch (JwtAuthenticationException e){
            setErrorResponse(response, e);
        }
    }

    private void setErrorResponse(
            HttpServletResponse response,
            JwtAuthenticationException e
    )throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(SC_UNAUTHORIZED);

        ErrorResponse errorResponse = ErrorResponse.of(
                UNAUTHORIZED,
                e.getCode(),
                e.getMessage()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
