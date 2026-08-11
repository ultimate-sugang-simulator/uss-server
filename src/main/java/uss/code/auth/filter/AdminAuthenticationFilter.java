package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.http.AdminEndpoint;

import java.io.IOException;

@RequiredArgsConstructor
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_HEADER = "access-token";
    private static final String ADMIN_ID_ATTRIBUTE = "admin-id";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

            final String accessToken = request.getHeader(ACCESS_TOKEN_HEADER);

            jwtProvider.validateAdminToken(accessToken);

            final Long adminId = jwtProvider.getAdminId(accessToken);
            request.setAttribute(ADMIN_ID_ATTRIBUTE, adminId);

            filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
        final String uri = request.getRequestURI();

        return !AdminEndpoint.isAdminPath(uri)
                || AdminEndpoint.isWhitelisted(uri, request.getMethod());
    }
}
