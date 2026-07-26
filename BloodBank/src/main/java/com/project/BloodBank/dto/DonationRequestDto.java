package com.project.BloodBank.dto;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.UrgencyLevel;

public class DonationRequestDto {

    private BloodGroup requestedBloodGroup;
    private int unitsNeeded;
    private String hospitalName;
    private String hospitalAddress;
    private UrgencyLevel urgencyLevel;
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