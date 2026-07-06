package com.smartcodenote.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.smartcodenote.config.properties.JwtProperties;
import com.smartcodenote.exception.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";

    private final JwtProperties jwtProperties;

    public String generateToken(Long userId) {
        Instant now = Instant.now();
        return JWT.create()
                .withClaim(CLAIM_USER_ID, userId)
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(jwtProperties.getExpireMinutes(), ChronoUnit.MINUTES)))
                .sign(algorithm());
    }

    public Long verifyAndGetUserId(String token) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "unauthorized");
        }
        try {
            JWTVerifier verifier = JWT.require(algorithm()).build();
            Long userId = verifier.verify(token).getClaim(CLAIM_USER_ID).asLong();
            if (userId == null) {
                throw new BusinessException(401, "invalid token");
            }
            return userId;
        } catch (JWTVerificationException e) {
            throw new BusinessException(401, "invalid token");
        }
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(jwtProperties.getSecret());
    }
}
