package com.a9.etutoring.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PostTargetId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "post_id", nullable = false)
    private UUID postId;

    @Column(name = "student_user_id", nullable = false)
    private UUID studentUserId;
}
