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

// Database access for accounts.
//
// There is no implementation of this interface anywhere, and none is needed: Spring Data reads the
// method names and writes the queries itself at startup. findByEmail becomes "where email = ?".
// The names are therefore not free - renaming one to something it cannot parse fails the whole
// application at boot.
//
// Extending JpaRepository<User, Long> also brings in save, findById, findAll, count and delete.
public interface UserRepository extends JpaRepository<User, Long> {

    // Sign-in and registration. Optional makes "no such account" impossible to ignore, and
    // existsByEmail is a COUNT rather than a full row fetch.
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    // Donor search. The "In" takes a collection, which is how compatibility works: the caller
    // passes every group whose blood the patient can accept, not just the exact match.
    List<User> findByBloodGroupAndActiveTrue(BloodGroup bloodGroup, Sort sort);
    Page<User> findByBloodGroupInAndActiveTrue(Collection<BloodGroup> bloodGroups, Pageable pageable);

    // ActiveTrue appears on every one of these on purpose. Deactivating is a soft delete, so the
    // rows are still there, and a query that forgot this would quietly resurrect them.
    Page<User> findAllByActiveTrue(Pageable pageable);
    Optional<User> findByIdAndActiveTrue(Long id);
    long countByActiveTrueAndRole(Role role);
}
