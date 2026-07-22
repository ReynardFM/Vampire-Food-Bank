package com.project.BloodBank.repository;

import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByBloodGroupAndActiveTrue(BloodGroup bloodGroup);
    List<User> findAllByActiveTrue();
    Optional<User> findByIdAndActiveTrue(Long id);
}
