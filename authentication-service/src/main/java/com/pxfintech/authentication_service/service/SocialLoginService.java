package com.pxfintech.authentication_service.service;

import com.pxfintech.authentication_service.dto.request.SocialLoginRequest;
import com.pxfintech.authentication_service.dto.response.SocialLoginResponse;

public interface SocialLoginService {

    SocialLoginResponse authenticateWithGoogle(String idToken, SocialLoginRequest request);

    SocialLoginResponse authenticateWithFacebook(String accessToken, SocialLoginRequest request);

    SocialLoginResponse authenticateWithApple(String idToken, String authorizationCode, SocialLoginRequest request);

    SocialLoginResponse linkSocialAccount(String userId, SocialLoginRequest request);

    void unlinkSocialAccount(String userId, String provider);

    boolean isSocialAccountLinked(String userId, String provider);
}