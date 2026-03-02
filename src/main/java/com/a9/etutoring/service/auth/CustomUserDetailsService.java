package com.a9.etutoring.service.auth;

import com.a9.etutoring.repository.UserRepository;
import com.a9.etutoring.security.UserPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsernameAndDeletedDateIsNull(username)
            .map(UserPrincipal::fromUser)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User " + username + " does not exist. Please sign up."
            ));
    }
}
