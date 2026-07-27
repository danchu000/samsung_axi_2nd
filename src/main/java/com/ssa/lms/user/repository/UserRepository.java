package com.ssa.lms.user.repository;

import com.ssa.lms.user.entity.Role;
import com.ssa.lms.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    long countByRole(Role role);
}
