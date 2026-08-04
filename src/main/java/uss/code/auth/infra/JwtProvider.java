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

    private final SecretKey secretKey;
    private final long accessTokenExpirationTime;

    public JwtProvider(
            @Value("${security.jwt.secret-key}") final String secretKey,
            @Value("${security.jwt.access-token-expiration-time}") long accessTokenExpirationTime
    ){
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
    }

    public AuthTokenResponse generateAuthToken(final long memberId) {
        return AuthTokenResponse.of(generateToken(memberId, accessTokenExpirationTime));
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
