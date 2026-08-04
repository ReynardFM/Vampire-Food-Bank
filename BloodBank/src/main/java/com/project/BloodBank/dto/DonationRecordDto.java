package com.project.BloodBank.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class DonationRecordDto {

    /* Same reason as UserProfileDto.dateOfBirth: <input type="date"> needs yyyy-MM-dd. */
    @NotNull(message = "Donation date is required")
    @PastOrPresent(message = "Donation date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate donationDate;

    @Min(value = 1, message = "At least one unit must be recorded")
    private int unitsDonated;

    private String location;

    /** Optional. When set, the linked request is marked FULFILLED. */
    private Long linkedRequestId;

    public DonationRecordDto() {
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public int getUnitsDonated() {
        return unitsDonated;
    }

    public void setUnitsDonated(int unitsDonated) {
        this.unitsDonated = unitsDonated;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getLinkedRequestId() {
        return linkedRequestId;
    }

    public void setLinkedRequestId(Long linkedRequestId) {
        this.linkedRequestId = linkedRequestId;
    }
}
