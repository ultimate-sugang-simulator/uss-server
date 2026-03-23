package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;
import uss.code.global.exception.domain.AtAuthenticationException;
import uss.code.global.exception.domain.JwtAuthenticationException;
import uss.code.global.exception.dto.response.ErrorResponse;

import java.io.IOException;

import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@RequiredArgsConstructor
public class AuthenticationExceptionFilter extends OncePerRequestFilter {

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
            setErrorResponse(response, e.getCode(), e.getMessage());
        }catch (final AtAuthenticationException e){
            setErrorResponse(response, e.getCode(), e.getMessage());
        }
    }

    private void setErrorResponse(
            final HttpServletResponse response,
            final int code,
            final String message
    ) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(SC_UNAUTHORIZED);

        final ErrorResponse errorResponse = ErrorResponse.of(code, message);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
