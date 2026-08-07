package com.project.BloodBank.controller;

import com.project.BloodBank.model.User;
import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import com.project.BloodBank.model.enums.Role;
import com.project.BloodBank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression cover for the profile date field.
 *
 * A LocalDate without @DateTimeFormat is rendered using a locale-dependent short pattern such as
 * 8/3/26. An <input type="date"> only accepts yyyy-MM-dd, so the browser discarded the value and
 * the field came up blank even though the date had been stored correctly. The round trip is what
 * broke, so both directions are asserted here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProfileDateBindingTest {

    private static final String EMAIL = "date-binding@test.local";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1994, 3, 12);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createDonor() {
        if (userRepository.findByEmail(EMAIL).isPresent()) {
            return;
        }
        User donor = new User();
        donor.setEmail(EMAIL);
        donor.setFullName("Date Tester");
        donor.setPassword(passwordEncoder.encode("Password123!"));
        donor.setRole(Role.USER);
        donor.setActive(true);
        donor.setPhoneNumber("+1 416 555 0000");
        donor.setBloodGroup(BloodGroup.A_POSITIVE);
        donor.setGender(Gender.FEMALE);
        donor.setAddress("1 Test Street, Toronto, ON");
        donor.setDateOfBirth(BIRTH_DATE);
        userRepository.save(donor);
    }

    /**
     * The failure users actually saw: the stored date has to come back out as yyyy-MM-dd in the
     * input's value attribute, or the browser silently renders an empty date picker.
     */
    @Test
    @WithMockUser(username = EMAIL)
    void editFormRendersTheStoredDateInIsoFormat() throws Exception {
        mockMvc.perform(get("/donor/profile-edit"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("profileDto",
                        hasProperty("dateOfBirth", equalTo(BIRTH_DATE))))
                .andExpect(content().string(containsString("value=\"1994-03-12\"")));
    }

    @Test
    @WithMockUser(username = EMAIL)
    void submittingAnIsoDateBindsAndPersists() throws Exception {
        LocalDate updated = LocalDate.of(1990, 12, 31);

        mockMvc.perform(post("/donor/profile-edit").with(csrf())
                        .param("fullName", "Date Tester")
                        .param("phoneNumber", "+1 416 555 0000")
                        .param("bloodGroup", "A_POSITIVE")
                        .param("dateOfBirth", "1990-12-31")
                        .param("gender", "FEMALE")
                        .param("address", "1 Test Street, Toronto, ON"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/donor/profile"));

        assertThat(userRepository.findByEmail(EMAIL))
                .get()
                .extracting(User::getDateOfBirth)
                .isEqualTo(updated);
    }

    /** A blank date must fail validation rather than bind as null and wipe the stored value. */
    @Test
    @WithMockUser(username = EMAIL)
    void submittingAnEmptyDateIsRejected() throws Exception {
        mockMvc.perform(post("/donor/profile-edit").with(csrf())
                        .param("fullName", "Date Tester")
                        .param("phoneNumber", "+1 416 555 0000")
                        .param("bloodGroup", "A_POSITIVE")
                        .param("dateOfBirth", "")
                        .param("gender", "FEMALE")
                        .param("address", "1 Test Street, Toronto, ON"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("profileDto", "dateOfBirth"));

        assertThat(userRepository.findByEmail(EMAIL))
                .get()
                .extracting(User::getDateOfBirth)
                .isEqualTo(BIRTH_DATE);
    }
}
