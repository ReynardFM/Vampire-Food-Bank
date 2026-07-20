package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.DonationStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name="donations")
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private User donor;
    private LocalDate donationData;
    private int unitsDonated;
    private String location;
    private DonationStatus donationStatus;
    private DonationRequest linkedRequest;

    public Donation() {}

    public long getId() {
        return id;
    }

    public User getDonor() {
        return donor;
    }

    public LocalDate getDonationData() {
        return donationData;
    }

    public int getUnitsDonated() {
        return unitsDonated;
    }

    public String getLocation() {
        return location;
    }

    public DonationStatus getDonationStatus() {
        return donationStatus;
    }

    public DonationRequest getLinkedRequest() {
        return linkedRequest;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setDonor(User donor) {
        this.donor = donor;
    }

    public void setDonationData(LocalDate donationData) {
        this.donationData = donationData;
    }

    public void setUnitsDonated(int unitsDonated) {
        this.unitsDonated = unitsDonated;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setDonationStatus(DonationStatus donationStatus) {
        this.donationStatus = donationStatus;
    }

    public void setLinkedRequest(DonationRequest linkedRequest) {
        this.linkedRequest = linkedRequest;
    }
}
