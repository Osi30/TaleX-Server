package com.talex.server.services.auth;

import com.talex.server.dtos.responses.auth.AuthResponse;
import com.talex.server.dtos.responses.auth.SsoHandoffResponse;

import java.util.UUID;

/**
 * Lets a mobile-authenticated user open the website already logged in: mobile
 * exchanges its Bearer token for a short-lived, single-use code, then the
 * website exchanges that code for a fresh session — no password re-entry.
 */
public interface SsoHandoffService {

    /** Issues a one-time code for the given (already Bearer-authenticated) account. Rate-limited. */
    SsoHandoffResponse createCode(UUID accountId);

    /** Consumes the code exactly once and mints a fresh token pair. Throws if invalid/expired/reused. */
    AuthResponse exchange(String code);
}
