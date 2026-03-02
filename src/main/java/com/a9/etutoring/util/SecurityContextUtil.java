package com.a9.etutoring.util;

import com.a9.etutoring.security.UserPrincipal;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        return principal instanceof UserPrincipal userPrincipal
            ? Optional.of(userPrincipal)
            : Optional.empty();
    }
}
