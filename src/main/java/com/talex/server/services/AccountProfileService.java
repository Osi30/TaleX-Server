package com.talex.server.services;

import com.talex.server.dtos.requests.ChangePasswordRequest;
import com.talex.server.dtos.requests.ForgotPasswordRequest;
import com.talex.server.dtos.requests.ResetPasswordRequest;
import com.talex.server.dtos.requests.UpdateProfileRequest;
import com.talex.server.dtos.responses.AccountProfileResponse;

import java.util.UUID;

public interface AccountProfileService {

    AccountProfileResponse getProfile(UUID accountId);

    AccountProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void changePassword(UUID accountId, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
