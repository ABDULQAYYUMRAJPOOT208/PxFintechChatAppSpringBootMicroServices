package com.pxfintech.user_service.security;

import com.pxfintech.user_service.model.User;
import com.pxfintech.user_service.repo.UserRepo;
import com.pxfintech.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//import org.springframework.security
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImp implements UserDetailsService {
        private final UserRepo userRepo;

        @Override
        @Transactional
        public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
                User user = userRepo.findByPhoneNumber(phoneNumber)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found with phone: " + phoneNumber));

                return org.springframework.security.core.userdetails.User.builder()
                                .username(user.getPhoneNumber())
                                .password(user.getPassword())
                                .roles("USER")
                                .accountExpired(false)
                                .accountLocked(false)
                                .credentialsExpired(false)
                                .disabled(!user.getIsVerified())
                                .build();
        }
}
