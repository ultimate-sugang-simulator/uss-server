package uss.code.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import uss.code.auth.infra.AtProvider;
import uss.code.global.http.WhitelistEndpoint;

import java.io.IOException;

@RequiredArgsConstructor
public class AdmissionTokenFilter extends OncePerRequestFilter {

    private final AtProvider atProvider;

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    )throws ServletException, IOException {

        final String atToken = request.getHeader("at-token");
        final long memberId = Long.parseLong(request.getAttribute("member-id").toString());

        atProvider.validateToken(atToken, memberId);

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
