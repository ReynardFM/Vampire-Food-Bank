package com.project.BloodBank.config;

import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.DonationRepository;
import com.project.BloodBank.repository.DonationRequestRepository;
import com.project.BloodBank.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The seeder runs on every start, so re-running it must never duplicate anything. Enabled here
 * only; the shared test profile turns it off so other tests start from an empty schema.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "bloodbank.seed.enabled=true")
class DatabaseSeederTest {

    @Autowired
    private DatabaseSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRequestRepository requestRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Test
    void seedingIsIdempotent() {
        long users = userRepository.count();
        long requests = requestRepository.count();
        long donations = donationRepository.count();

        assertThat(users).isPositive();
        assertThat(requests).isPositive();
        assertThat(donations).isPositive();

        // The context start already ran it once; a second pass must be a no-op.
        seeder.run();

        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(requestRepository.count()).isEqualTo(requests);
        assertThat(donationRepository.count()).isEqualTo(donations);
    }

    @Test
    void anAdministratorExistsSoTheAdminAreaIsReachable() {
        assertThat(userRepository.countByActiveTrueAndRole(Role.ADMIN)).isPositive();
    }

    @Test
    void everyBloodGroupHasAtLeastOneDonorSoSearchIsNeverEmpty() {
        for (com.project.BloodBank.model.enums.BloodGroup group
                : com.project.BloodBank.model.enums.BloodGroup.values()) {
            assertThat(userRepository.findByBloodGroupAndActiveTrue(
                    group, org.springframework.data.domain.Sort.by("fullName")))
                    .as("donors with blood group %s", group)
                    .isNotEmpty();
        }
    }
}
