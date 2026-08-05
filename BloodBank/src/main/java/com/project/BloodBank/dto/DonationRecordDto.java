package com.project.BloodBank.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// What the record-donation form submits. Filled in by an administrator after a collection.
public class DonationRecordDto {

    // @PastOrPresent because a donation is something that already happened - this records history,
    // it does not book an appointment.
    //
    // @DateTimeFormat for the same reason as UserProfileDto.dateOfBirth: <input type="date"> only
    // understands yyyy-MM-dd, and without it the field renders blank.
    @NotNull(message = "Donation date is required")
    @PastOrPresent(message = "Donation date cannot be in the future")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate donationDate;

    @Min(value = 1, message = "At least one unit must be recorded")
    private int unitsDonated;

    private String location;

    // Optional: null for a walk-in donation. When set, DonationService marks that request
    // FULFILLED - but only after checking it is still approved and the blood is compatible.
    //
    // An id rather than the request itself, because a form can only post text. The service looks it
    // up, which is also where it gets checked.
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
