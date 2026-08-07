package com.project.BloodBank.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sort.by() accepts any string and an unknown property raises PropertyReferenceException, so a
 * hand-edited query string would be a 500 without the controllers' whitelists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSortingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void donorListAcceptsAWhitelistedSort() throws Exception {
        mockMvc.perform(get("/admin/donors").param("sort", "email").param("dir", "desc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "email"))
                .andExpect(model().attribute("dir", "desc"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void donorListFallsBackWhenTheSortFieldIsUnknown() throws Exception {
        mockMvc.perform(get("/admin/donors").param("sort", "password"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "fullName"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void pendingQueueFallsBackWhenTheSortFieldIsUnknown() throws Exception {
        mockMvc.perform(get("/admin/pending").param("sort", "notes"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "requestDate"));
    }

    /** Urgency is requested by its URL name but ordered on the mirrored severity column. */
    @Test
    @WithMockUser(roles = "ADMIN")
    void pendingQueueAcceptsUrgencyAndEchoesTheUrlFacingName() throws Exception {
        mockMvc.perform(get("/admin/pending").param("sort", "urgencyLevel").param("dir", "desc"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("sort", "urgencyLevel"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminPagesStayClosedToDonors() throws Exception {
        mockMvc.perform(get("/admin/donors")).andExpect(status().isForbidden());
    }
}
