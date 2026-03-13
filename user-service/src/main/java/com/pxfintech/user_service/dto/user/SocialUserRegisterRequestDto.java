package com.pxfintech.user_service.dto.user;

import lombok.Data;

@Data
public class SocialUserRegisterRequestDto {
    private String fullName;
    private String email;
    private String profilePicture;
    private String authProvider;
    private Boolean isVerified;
}
