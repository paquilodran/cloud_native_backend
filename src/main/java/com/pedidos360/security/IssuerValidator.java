package com.pedidos360.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

/**
 * Acepta issuer v2.0 y v1.0 de Azure AD (tokens de API custom suelen venir como v1).
 */
public class IssuerValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> trustedIssuers;

    public IssuerValidator(String tenantId) {
        this.trustedIssuers = Set.of(
                "https://login.microsoftonline.com/" + tenantId + "/v2.0",
                "https://sts.windows.net/" + tenantId + "/"
        );
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String issuer = token.getIssuer() != null ? token.getIssuer().toString() : "";
        if (trustedIssuers.contains(issuer) || trustedIssuers.contains(issuer + "/")) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Issuer no confiable: " + issuer, null));
    }
}
