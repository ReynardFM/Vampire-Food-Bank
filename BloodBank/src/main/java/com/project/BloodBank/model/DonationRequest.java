package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.DonationStatus;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.UrgencyLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "donation_requests")
public class DonationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup requestedBloodGroup;

    @Column(nullable = false)
    private int unitsNeeded;

    @Column(nullable = false)
    private String hospitalName;

    @Column
    private String hospitalAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(nullable = false)
    private LocalDateTime requestDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "linkedRequest")
    private List<Donation> donations = new ArrayList<>();

    public DonationRequest() {
    }

    public DonationRequest(
            BloodGroup requestedBloodGroup,
            int unitsNeeded,
            String hospitalName,
            String hospitalAddress,
            UrgencyLevel urgencyLevel,
            User requestedBy,
            String notes
    ) {
        this.requestedBloodGroup = requestedBloodGroup;
        this.unitsNeeded = unitsNeeded;
        this.hospitalName = hospitalName;
        this.hospitalAddress = hospitalAddress;
        this.urgencyLevel = urgencyLevel;
        this.requestedBy = requestedBy;
        this.notes = notes;
        this.status = RequestStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    @PrePersist
    public void setDefaultValues() {
        if (status == null) {
            status = DonationStatus.PENDING;
        }

        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
    }

    public void addDonation(Donation donation) {
        donations.add(donation);
        donation.setLinkedRequest(this);
    }

    public void removeDonation(Donation donation) {
        donations.remove(donation);
        donation.setLinkedRequest(null);
    }

    public Long getId() {
        return id;
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

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User requestedBy) {
        this.requestedBy = requestedBy;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Donation> getDonations() {
        return donations;
    }

    public void setDonations(List<Donation> donations) {
        this.donations = donations;
    }
}