package com.project.BloodBank.service;

import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User save(String email, String name, Role role, boolean active) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(name);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(role);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Test
    void deactivatingADonorWorks() {
        User donor = save("donor@test.local", "Test Donor", Role.DONOR, true);

        userService.deactivateUser(donor.getId());

        assertThat(userRepository.findById(donor.getId()))
                .get()
                .extracting(User::isActive)
                .isEqualTo(false);
    }

    /**
     * Nothing in the app can create or re-enable an administrator, so deactivating one is a
     * one-way door. Guarded in the service so it holds regardless of caller.
     */
    @Test
    void deactivatingAnAdministratorIsRefused() {
        User admin = save("admin@test.local", "Test Admin", Role.ADMIN, true);

        assertThatThrownBy(() -> userService.deactivateUser(admin.getId()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findById(admin.getId()))
                .get()
                .extracting(User::isActive)
                .isEqualTo(true);
    }

    @Test
    void activeUserListingExcludesDeactivatedAccounts() {
        save("active@test.local", "Active Donor", Role.DONOR, true);
        save("inactive@test.local", "Inactive Donor", Role.DONOR, false);

        List<String> emails = userService
                .getAllActiveUsers(PageRequest.of(0, 50, Sort.by("fullName")))
                .getContent().stream().map(User::getEmail).toList();

        assertThat(emails).contains("active@test.local");
        assertThat(emails).doesNotContain("inactive@test.local");
    }

    @Test
    void registeringStoresAHashedPasswordNotThePlainText() {
        com.project.BloodBank.dto.UserRegistrationDto dto =
                new com.project.BloodBank.dto.UserRegistrationDto();
        dto.setFullName("New Person");
        dto.setEmail("new@test.local");
        dto.setPassword("Password123!");

        User created = userService.register(dto);

        assertThat(created.getPassword()).isNotEqualTo("Password123!");
        assertThat(passwordEncoder.matches("Password123!", created.getPassword())).isTrue();
        assertThat(created.getRole()).isEqualTo(Role.DONOR);
        assertThat(created.isActive()).isTrue();
    }
}
