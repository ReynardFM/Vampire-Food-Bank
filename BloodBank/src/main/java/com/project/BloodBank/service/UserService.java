package com.project.BloodBank.service;

import com.project.BloodBank.exception.EmailAlreadyExistsException;
import com.project.BloodBank.exception.ResourceNotFoundException;
import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + dto.getEmail());
        }

        User user = new User();
        user.setfullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.DONOR);
        user.setActive(true);

        return userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, UserProfileDto dto) {
        User user = getUserById(userId);

        user.setPhoneNumber(dto.getPhoneNumber());
        user.setBloodGroup(dto.getBloodGroup());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setGender(dto.getGender());
        user.setAddress(dto.getAddress());

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> searchDonorsByBloodGroup(BloodGroup bloodGroup) {
        return userRepository.findByBloodGroupAndActiveTrue(bloodGroup);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        user.setActive(false);
        userRepository.save(user);
    }
}
