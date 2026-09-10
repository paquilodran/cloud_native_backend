package com.pedidos360.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Azure AD puede poner en "aud" el client id, api://client-id o el App ID URI.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final Set<String> allowedAudiences;

    public AudienceValidator(Collection<String> allowedAudiences) {
        this.allowedAudiences = new HashSet<>();
        for (String audience : allowedAudiences) {
            if (audience != null && !audience.isBlank()) {
                this.allowedAudiences.add(audience.trim());
            }
        }
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (allowedAudiences.isEmpty()) {
            return OAuth2TokenValidatorResult.success();
        }
        for (String audience : token.getAudience()) {
            if (allowedAudiences.contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "El audience del JWT no coincide con Pedidos360-API", null));
    }
}
