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
import uss.code.global.exception.domain.RestApiException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static uss.code.global.exception.domain.ExceptionCode.*;

@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String ADMIN_ROLE = "ADMIN";

    private final SecretKey secretKey;
    private final long accessTokenExpirationTime;
    private final long adminAccessTokenExpirationTime;

    public JwtProvider(
            @Value("${security.jwt.secret-key}") final String secretKey,
            @Value("${security.jwt.access-token-expiration-time}") long accessTokenExpirationTime,
            @Value("${security.jwt.admin-access-token-expiration-time}") long adminAccessTokenExpirationTime
    ){
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.adminAccessTokenExpirationTime = adminAccessTokenExpirationTime;
    }

    public AuthTokenResponse generateAuthToken(final long memberId) {
        return AuthTokenResponse.of(generateToken(memberId, accessTokenExpirationTime));
    }

    public String generateAdminToken(final long adminId) {
        final Date now = new Date();
        final Date validityDate = new Date(now.getTime() + adminAccessTokenExpirationTime);

        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim(ROLE_CLAIM, ADMIN_ROLE)
                .issuedAt(now)
                .expiration(validityDate)
                .signWith(secretKey)
                .compact();
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

    public void validateToken(final String accessToken){
        if(accessToken == null)
            throw new JwtTokenMissingException(MISSING_ACCESS_TOKEN);

        validateAccessToken(accessToken);
    }

    public Long getMemberId(final String token){
        final String memberId = parseJwt(token)
                .getPayload()
                .getSubject();

        return Long.valueOf(memberId);
    }

    public Long getMemberIdAllowingExpiration(final String accessToken){
        if(accessToken == null)
            throw new JwtTokenMissingException(MISSING_ACCESS_TOKEN);

        try {
            return Long.valueOf(parseJwt(accessToken).getPayload().getSubject());
        } catch (ExpiredJwtException e) {
            return Long.valueOf(e.getClaims().getSubject());
        } catch (MalformedJwtException e) {
            throw new JwtTokenInvalidException(INVALID_FORM_ACCESS_TOKEN);
        } catch (SignatureException e) {
            throw new JwtTokenInvalidException(INVALID_SIGNATURE_ACCESS_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtTokenInvalidException(INVALID_ACCESS_TOKEN);
        }
    }

    public void validateAdminToken(final String accessToken){
        validateToken(accessToken);

        if (!isAdminToken(accessToken))
            throw new RestApiException(ADMIN_ACCESS_DENIED);
    }

    public Long getAdminId(final String accessToken){
        return getMemberId(accessToken);
    }

    public Long getAdminIdAllowingExpiration(final String accessToken){
        return getMemberIdAllowingExpiration(accessToken);
    }

    public boolean isAdminToken(final String accessToken){
        return ADMIN_ROLE.equals(extractRole(accessToken));
    }

    private String extractRole(final String accessToken){
        try {
            return parseJwt(accessToken).getPayload().get(ROLE_CLAIM, String.class);
        } catch (ExpiredJwtException e) {
            return e.getClaims().get(ROLE_CLAIM, String.class);
        }
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
}
