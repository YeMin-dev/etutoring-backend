package com.a9.etutoring.service.auth;

import com.a9.etutoring.domain.dto.auth.AuthLoginRequest;
import com.a9.etutoring.domain.dto.auth.AuthResponse;
import com.a9.etutoring.domain.dto.auth.AuthSignupRequest;
import com.a9.etutoring.domain.dto.auth.ForgotPasswordRequest;
import com.a9.etutoring.domain.dto.auth.ResetPasswordRequest;

public interface AuthService {

    AuthResponse signup(AuthSignupRequest request);

    AuthResponse login(AuthLoginRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
