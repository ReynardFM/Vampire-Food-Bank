package com.project.BloodBank.dto;

import java.time.LocalDate;

public class DonationRecordDto {

    private LocalDate donationDate;
    private int unitsDonated;
    private String location;
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
