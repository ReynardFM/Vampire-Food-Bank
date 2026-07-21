package com.project.BloodBank.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name="donations")
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="donor_id", nullable=false)
    private User donor;

    @Column(nullable=false)
    private LocalDate donationDate;

    @Column(nullable=false)
    private int unitsDonated;

    @Column
    private String location;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="linked_request_id", nullable=true)
    private DonationRequest linkedRequest;

    public Donation() {}

    // getters
    public Long getId() {
        return id;
    }

    public User getDonor() {
        return donor;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public int getUnitsDonated() {
        return unitsDonated;
    }

    public String getLocation() {
        return location;
    }

    public DonationRequest getLinkedRequest() {
        return linkedRequest;
    }

    // setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setDonor(User donor) {
        this.donor = donor;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public void setUnitsDonated(int unitsDonated) {
        this.unitsDonated = unitsDonated;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLinkedRequest(DonationRequest linkedRequest) {
        this.linkedRequest = linkedRequest;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if(!(o instanceof Donation)) return false;

        Donation donation = (Donation) o;

        return id != null && id.equals(donation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Donation{" +
                "id=" + id +
                ", donationDate=" + donationDate +
                ", unitsDonated=" + unitsDonated +
                ", location='" + location + '\'' +
                '}';
    }
}
