package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.exception.domain.JwtTokenInvalidException;
import uss.code.global.http.AdminEndpoint;
import uss.code.global.http.WhitelistEndpoint;

import java.io.IOException;

import static uss.code.global.exception.domain.ExceptionCode.INVALID_ACCESS_TOKEN;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_HEADER = "access-token";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

            final String accessToken = request.getHeader(ACCESS_TOKEN_HEADER);

            jwtProvider.validateToken(accessToken);

            if (jwtProvider.isAdminToken(accessToken))
                throw new JwtTokenInvalidException(INVALID_ACCESS_TOKEN);

            final Long memberId = jwtProvider.getMemberId(accessToken);
            request.setAttribute("member-id", memberId);

            filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
        final String uri = request.getRequestURI();

        return AdminEndpoint.isAdminPath(uri)
                || WhitelistEndpoint.isWhitelisted(uri, request.getMethod());
    }
}
