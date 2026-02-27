package com.pxfintech.user_service.utlls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {
    @Value("${otp.length}")
    private int otpLength;

    private final SecureRandom random = new SecureRandom();

    public String generateOtp()
    {
        StringBuilder otp = new StringBuilder(otpLength);
        for(int i = 0; i<otpLength; i++)
        {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }
}
