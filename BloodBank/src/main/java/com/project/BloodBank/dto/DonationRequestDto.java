package com.project.BloodBank.dto;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.UrgencyLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// What the "raise a request" form is allowed to submit.
//
// Missing on purpose: status and requestedBy. Status is defaulted to PENDING by the entity, and the
// requester is taken from the session - so nobody can raise a pre-approved request, or one in
// somebody else's name.
public class DonationRequestDto {

    @NotNull(message = "Blood group is required")
    private BloodGroup requestedBloodGroup;

    // A primitive int, so it cannot be null and @NotNull would be meaningless. @Min does the work
    // instead - and it is needed, since an unfilled number field binds to 0.
    @Min(value = 1, message = "At least 1 unit must be requested")
    private int unitsNeeded;

    @NotBlank(message = "Hospital name is required")
    private String hospitalName;

    @NotBlank(message = "Hospital address is required")
    private String hospitalAddress;

    @NotNull(message = "Urgency level is required")
    private UrgencyLevel urgencyLevel;

    // The only optional field, hence no annotation at all.
    private String notes;

    public DonationRequestDto() {
    }

    public BloodGroup getRequestedBloodGroup() {
        return requestedBloodGroup;
    }

    public void setRequestedBloodGroup(BloodGroup requestedBloodGroup) {
        this.requestedBloodGroup = requestedBloodGroup;
    }

    public int getUnitsNeeded() {
        return unitsNeeded;
    }

    public void setUnitsNeeded(int unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalAddress() {
        return hospitalAddress;
    }

    public void setHospitalAddress(String hospitalAddress) {
        this.hospitalAddress = hospitalAddress;
    }

    public UrgencyLevel getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}