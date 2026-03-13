package com.a9.etutoring.repository;

import com.a9.etutoring.domain.enums.UserRole;
import com.a9.etutoring.domain.model.User;
import java.util.Collection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndDeletedDateIsNull(UUID id);

    boolean existsByUsernameAndDeletedDateIsNull(String username);

    boolean existsByEmailAndDeletedDateIsNull(String email);

    Optional<User> findByUsernameAndDeletedDateIsNull(String username);

    Optional<User> findByEmailAndDeletedDateIsNull(String email);

    List<User> findAllByDeletedDateIsNull();

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
