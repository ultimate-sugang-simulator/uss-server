package uss.code.auth.infra;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uss.code.auth.dto.response.AuthTokenResponse;
import uss.code.global.exception.domain.JwtTokenExpiredException;
import uss.code.global.exception.domain.JwtTokenInvalidException;
import uss.code.global.exception.domain.JwtTokenMissingException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static uss.code.global.exception.domain.ExceptionCode.*;

@Component
public class JwtProvider {

    private static final int NO_SUBJECT = -1;

    private final SecretKey secretKey;
    private final long accessTokenExpirationTime;
    private final long refreshTokenExpirationTime;

    public JwtProvider(
            @Value("${security.jwt.secret-key}") final String secretKey,
            @Value("${security.jwt.access-token-expiration-time}") long accessTokenExpirationTime,
            @Value("${security.jwt.refresh-token-expiration-time}") long refreshTokenExpirationTime
    ){
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public AuthTokenResponse generateAuthTokens(final long memberId) {
        final String accessToken = generateToken(memberId, accessTokenExpirationTime);
        final String refreshToken = generateToken(NO_SUBJECT, refreshTokenExpirationTime);

        return AuthTokenResponse.of(
                accessToken,
                refreshToken
        );
    }

    private String generateToken(
            final long memberId,
            final long validityTime
    ) {
        final Date now = new Date();
        final Date validityDate = new Date(now.getTime() + validityTime);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(validityDate)
                .signWith(secretKey)
                .compact();
    }

    public void validateTokens(
            final String accessToken,
            final String refreshToken
    ){
        if(accessToken == null)
            throw new JwtTokenMissingException(MISSING_ACCESS_TOKEN);

        if(refreshToken == null)
            throw new JwtTokenMissingException(MISSING_REFRESH_TOKEN);

        validateAccessToken(accessToken);
        validateRefreshToken(refreshToken);
    }

    public Long getMemberId(final String token){
        final String memberId = parseJwt(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(memberId);
    }

    private Jws<Claims> parseJwt(final String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }

    private void validateAccessToken(final String accessToken){
        try {
            parseJwt(accessToken);
        } catch (ExpiredJwtException e) {
            throw new JwtTokenExpiredException(EXPIRED_ACCESS_TOKEN);
        } catch (MalformedJwtException e) {
            throw new JwtTokenInvalidException(INVALID_FORM_ACCESS_TOKEN);
        } catch (SignatureException e) {
            throw new JwtTokenInvalidException(INVALID_SIGNATURE_ACCESS_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenInvalidException(INVALID_ACCESS_TOKEN);
        }
    }

    private void validateRefreshToken(final String refreshToken){
        try {
            parseJwt(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new JwtTokenExpiredException(EXPIRED_REFRESH_TOKEN);
        } catch (MalformedJwtException e) {
            throw new JwtTokenInvalidException(INVALID_FORM_REFRESH_TOKEN);
        } catch (SignatureException e) {
            throw new JwtTokenInvalidException(INVALID_SIGNATURE_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenInvalidException(INVALID_REFRESH_TOKEN);
        }
    }
}
