package com.a9.etutoring.repository;

import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import java.util.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndDeletedDateIsNull(UUID id);

    boolean existsByUsernameAndDeletedDateIsNull(String username);

    boolean existsByEmailAndDeletedDateIsNull(String email);

    Optional<User> findByUsernameAndDeletedDateIsNull(String username);

    Optional<User> findByEmailAndDeletedDateIsNull(String email);

    Optional<User> findByPasswordResetTokenAndPasswordResetExpiresAtAfterAndDeletedDateIsNull(String token, Instant expiresAtAfter);

    List<User> findAllByDeletedDateIsNull();

    Page<User> findAllByDeletedDateIsNullAndRole(UserRole role, Pageable pageable);

    boolean existsByDeletedDateIsNullAndRole(UserRole role);

    boolean existsByDeletedDateIsNullAndRoleAndIdNot(UserRole role, UUID id);

    Optional<User> findFirstByDeletedDateIsNullAndRoleOrderByCreatedDateAsc(UserRole role);

    List<User> findAllByIdInAndDeletedDateIsNull(Collection<UUID> ids);

        @Query(
                """
                select u
                from User u
                where u.deletedDate is null
                    and u.role = :role
                    and (u.lastInteractionDate is null or u.lastInteractionDate < :cutoff)
                order by u.lastInteractionDate asc
                """
        )
        List<User> findInactiveUsersByRole(@Param("role") UserRole role, @Param("cutoff") Instant cutoff);
}
