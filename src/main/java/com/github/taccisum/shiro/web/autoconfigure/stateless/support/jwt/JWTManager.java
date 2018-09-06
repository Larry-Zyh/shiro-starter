package com.github.taccisum.shiro.web.autoconfigure.stateless.support.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

/**
 * @author tac - liaojf@cheegu.com
 * @since 2018/9/4
 */
public class JWTManager {
    private static final int DEFAULT_EXPIRES_MINUTES = 60 * 24;

    private int expiresMinutes;
    private JWTAlgorithmProvider algorithm;
    private String issuer = "access_token";
    private PayloadTemplate payloadTemplate;

    public JWTManager(String issuer, PayloadTemplate payloadTemplate) {
        this(issuer, payloadTemplate, DEFAULT_EXPIRES_MINUTES);
    }

    public JWTManager(String issuer, PayloadTemplate payloadTemplate, int expiresMinutes) {
        this(issuer, payloadTemplate, expiresMinutes, new DefaultJWTAlgorithmProvider());
    }

    public JWTManager(String issuer, PayloadTemplate payloadTemplate, int expiresMinutes, JWTAlgorithmProvider algorithm) {
        this.issuer = issuer;
        this.payloadTemplate = payloadTemplate;
        this.expiresMinutes = expiresMinutes;
        this.algorithm = algorithm;
    }

    public String create(Payload payload) {
        payload.forEach((k, v) -> {
            payloadTemplate.check().hasField(k, v);
        });
        payloadTemplate.check().missingFields(payload);

        JWTCreator.Builder builder = JWT.create()
                .withIssuer(issuer)
                .withExpiresAt(calculateExpiresTime(expiresMinutes));

        payload.forEach((k, v) -> {
            if (v instanceof Boolean) {
                builder.withClaim(k, (Boolean) v);
            } else if (v instanceof Integer) {
                builder.withClaim(k, (Integer) v);
            } else if (v instanceof Long) {
                builder.withClaim(k, (Long) v);
            } else if (v instanceof Double) {
                builder.withClaim(k, (Double) v);
            } else if (v instanceof Date) {
                builder.withClaim(k, (Date) v);
            } else if (v instanceof String) {
                builder.withClaim(k, (String) v);
            } else {
                builder.withClaim(k, v.toString());
            }
        });
        return builder
                .withJWTId(newJWTId())
                .sign(algorithm.get());
    }

    public DecodedJWT verify(String jwt) throws JWTVerificationException {
        JWTVerifier verifier = JWT.require(algorithm.get())
                .withIssuer(issuer)
                .build();
        return verifier.verify(jwt);
    }

    public Payload parsePayload(DecodedJWT decodedJWT) {
        Payload payload = new Payload();
        payloadTemplate.getFieldMap().forEach((k, v) -> {
            if (Objects.equals(v, Boolean.class)) {
                payload.put(k, decodedJWT.getClaim(k).asBoolean());
            } else if (Objects.equals(v, Integer.class)) {
                payload.put(k, decodedJWT.getClaim(k).asInt());
            } else if (Objects.equals(v, Long.class)) {
                payload.put(k, decodedJWT.getClaim(k).asLong());
            } else if (Objects.equals(v, Double.class)) {
                payload.put(k, decodedJWT.getClaim(k).asDouble());
            } else if (Objects.equals(v, Date.class)) {
                payload.put(k, decodedJWT.getClaim(k).asDate());
            } else if (Objects.equals(v, String.class)) {
                payload.put(k, decodedJWT.getClaim(k).asString());
            } else {
                payload.put(k, decodedJWT.getClaim(k).asString());
            }
        });
        return payload;
    }

    public Payload verifyAndParsePayload(String jwt) throws JWTVerificationException {
        return parsePayload(verify(jwt));
    }

    static Date calculateExpiresTime(int expiresMinutes) {
        return new Date(new Date().getTime() + expiresMinutes * 60 * 1000);
    }

    private static String newJWTId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
