package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import uss.code.auth.infra.JwtProvider;
import uss.code.global.http.WhitelistEndpoint;

import java.io.IOException;

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

            final Long memberId = jwtProvider.getMemberId(accessToken);
            request.setAttribute("member-id", memberId);

            filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
        return WhitelistEndpoint.isWhitelisted(
                request.getRequestURI(),
                request.getMethod()
        );
    }
}
