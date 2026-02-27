package com.pxfintech.user_service.service;

import com.pxfintech.user_service.utlls.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final RedisTemplate<String, String> redisTemplate;
    private final OtpGenerator otpGenerator;

    @Value("${otp.expiry}")
    private int otpExpiry;

    private  static final String OTP_PREFIX = "otp:";

    public String generateAndSendOTP(String phoneNumber)
    {
        String otp = otpGenerator.generateOtp();
        String key = OTP_PREFIX + phoneNumber;

        redisTemplate.opsForValue().set(key,otp,otpExpiry, TimeUnit.SECONDS);

        log.info("OTP genrated for phoneNumber: {}",phoneNumber);
        // to be implemented to send otp

        log.info("Your OTP is: {}", otp);
        return  otp;

    }

    public boolean validateOTP(String phoneNumber, String otp)
    {
        String key = OTP_PREFIX + phoneNumber;
        String storedOtp = redisTemplate.opsForValue().get(key);
        if( storedOtp != null && storedOtp.equals(otp))
        {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }
}
