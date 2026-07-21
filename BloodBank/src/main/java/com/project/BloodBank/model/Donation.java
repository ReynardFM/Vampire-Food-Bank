package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.DonationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="donations",
        indexes={
                @Index(name="idx_donation_date", columnList="donation_date"),
                @Index(name="idx_donation_donor", columnList="donor_id")
        })
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
    @Min(1)
    private int unitsDonated;

    @Column
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DonationStatus status;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="linked_request_id", nullable=true)
    private DonationRequest linkedRequest;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

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

    public DonationStatus getStatus() {
        return status;
    }

    public DonationRequest getLinkedRequest() {
        return linkedRequest;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public void setLinkedRequest(DonationRequest linkedRequest) {
        this.linkedRequest = linkedRequest;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Donation{" +
                "id=" + id +
                ", donationDate=" + donationDate +
                ", unitsDonated=" + unitsDonated +
                ", location='" + location + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
