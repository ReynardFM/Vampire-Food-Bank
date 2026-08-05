package com.project.BloodBank.service;

import com.project.BloodBank.dto.UserProfileDto;
import com.project.BloodBank.dto.UserRegistrationDto;
import com.project.BloodBank.exception.EmailAlreadyExistsException;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Everything to do with accounts: registering, editing a profile, searching, deactivating.
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Creates a new account from the registration form.
    @Transactional
    public User register(UserRegistrationDto dto) {

        // Checked here so the user gets a readable message. The unique constraint on the column is
        // the real guarantee - two people registering at the same instant both pass this check.
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + dto.getEmail());
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());

        // Hashed before it is ever stored. BCrypt is deliberately slow and salts each hash, so the
        // same password produces a different string every time and cannot be reversed.
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Role and active are set here rather than taken from the form. Letting the browser supply
        // either would mean anyone could register themselves an administrator.
        user.setRole(Role.DONOR);
        user.setActive(true);

        return userRepository.save(user);
    }

    // Applies the profile form. Only these six fields, so email, role and active cannot be reached
    // through it - the DTO simply has nowhere to put them.
    //
    // No explicit save is needed for the change to stick inside a transaction, since the entity is
    // managed and Hibernate writes changes at commit. It is called anyway for clarity.
    @Transactional
    public User updateProfile(Long userId, UserProfileDto dto) {
        User user = getUserById(userId);

        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setBloodGroup(dto.getBloodGroup());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress());

        return userRepository.save(user);
    }

    // Deactivated accounts are treated as gone, so this refuses to find them.
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    // Everyone who could give blood to a patient of this group, not only people with the identical
    // group. compatibleDonors() is what widens it - see BloodGroup.
    @Transactional(readOnly = true)
    public Page<User> searchCompatibleDonors(BloodGroup recipientGroup, Pageable pageable) {
        return userRepository.findByBloodGroupInAndActiveTrue(recipientGroup.compatibleDonors(), pageable);
    }

    // The signed-in user, freshly loaded rather than taken from the session.
    //
    // SecurityContextHolder holds the authentication for the current request, in a thread-local, so
    // this works without the caller passing anything in. getName() returns whatever
    // User.getUsername() gave at sign-in, which is the email.
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    // Soft delete: the row stays, the flag flips. Note findById rather than getUserById, so an
    // already-deactivated account can still be found rather than reported missing.
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        // Nothing in this application creates or re-enables an administrator, so deactivating one
        // is a one-way door. Enforced here rather than in the controller so it holds for every
        // caller. The controller separately stops an admin deactivating themselves.
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Administrator accounts cannot be deactivated.");
        }

        user.setActive(false);
        userRepository.save(user);
    }

    // Every active account, administrators included. Safe to list them now that deactivateUser
    // refuses ROLE_ADMIN outright, so an admin row can be shown without being a hazard.
    @Transactional(readOnly = true)
    public Page<User> getAllActiveUsers(Pageable pageable) {
        return userRepository.findAllByActiveTrue(pageable);
    }
}
