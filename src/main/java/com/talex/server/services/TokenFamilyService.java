package com.talex.server.services;

import java.util.UUID;

/**
 * Manages refresh token families in Redis.
 * Each login session creates a token family — only the latest refresh token
 * in that family is valid at any time (rotation with reuse detection).
 */
public interface TokenFamilyService {

    /**
     * Creates a new token family for the given account.
     * Returns the refresh token string (format: familyId.randomSecret).
     */
    String createFamily(UUID accountId);

    /**
     * Validates the refresh token and rotates to a new one.
     * Returns the new refresh token, or throws if invalid/reuse detected.
     */
    String validateAndRotate(String refreshToken);

    /**
     * Extracts the accountId stored in the token family.
     */
    UUID extractAccountId(String refreshToken);

    /**
     * Deletes a single token family (single-device logout).
     */
    void deleteFamily(String refreshToken);

    /**
     * Deletes ALL token families for an account (logout everywhere).
     */
    void deleteAllFamilies(UUID accountId);
}
