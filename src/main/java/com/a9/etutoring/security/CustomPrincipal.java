package com.a9.etutoring.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public record CustomPrincipal(UUID userId, String email) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
