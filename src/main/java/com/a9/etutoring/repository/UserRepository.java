package com.a9.etutoring.repository;

import com.a9.etutoring.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByIdAndDeletedDateIsNull(UUID id);

    boolean existsByUsernameAndDeletedDateIsNull(String username);

    boolean existsByEmailAndDeletedDateIsNull(String email);

    Optional<User> findByUsernameAndDeletedDateIsNull(String username);

    Optional<User> findByEmailAndDeletedDateIsNull(String email);

    List<User> findAllByDeletedDateIsNull();
}
