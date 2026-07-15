package com.talex.server.services.auth;

import com.talex.server.dtos.requests.auth.ChangePasswordRequest;
import com.talex.server.dtos.requests.auth.ForgotPasswordRequest;
import com.talex.server.dtos.requests.auth.ResetPasswordRequest;
import com.talex.server.dtos.requests.auth.UpdateProfileRequest;
import com.talex.server.dtos.responses.auth.AccountProfileResponse;

import java.util.UUID;

public interface AccountProfileService {

    AccountProfileResponse getProfile(UUID accountId);

    AccountProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void changePassword(UUID accountId, ChangePasswordRequest request);

    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
