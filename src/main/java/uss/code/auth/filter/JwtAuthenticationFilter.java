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

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {

            String accessToken = request.getHeader("access-token");
            String refreshToken = request.getHeader("refresh-token");

            jwtProvider.validateTokens(accessToken, refreshToken);

            Long memberId = jwtProvider.getMemberId(accessToken);
            request.setAttribute("member-id", memberId);

            filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return WhitelistEndpoint.isWhitelisted(
                request.getRequestURI(),
                request.getMethod()
        );
    }
}
