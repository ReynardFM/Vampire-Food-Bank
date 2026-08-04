package com.project.BloodBank.repository;

import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByBloodGroupAndActiveTrue(BloodGroup bloodGroup, Sort sort);
    Page<User> findByBloodGroupInAndActiveTrue(Collection<BloodGroup> bloodGroups, Pageable pageable);
    Page<User> findAllByActiveTrue(Pageable pageable);
    Optional<User> findByIdAndActiveTrue(Long id);
    long countByActiveTrueAndRole(Role role);
}
