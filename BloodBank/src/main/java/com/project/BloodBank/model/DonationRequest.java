package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.UrgencyLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "donation_requests"
)
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

    @Column()
    private String hospitalAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "requested_by_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_donationrequest_requestedby",
                    foreignKeyDefinition = "FOREIGN KEY (requested_by_id) REFERENCES users(id) ON DELETE RESTRICT"
            )
    )
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestDate;

    @Column(columnDefinition = "TEXT")
    private String notes;


    @OneToMany(
            mappedBy = "linkedRequest",
            fetch = FetchType.LAZY
    )
    private List<Donation> donations = new ArrayList<>();

    public DonationRequest() {
    }

    @PrePersist
    public void setDefaultValues() {
        if (status == null) {
            status = RequestStatus.PENDING;
        }

        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }
    }

    public void addDonation(Donation donation) {
        if (donation != null && !donations.contains(donation)) {
            donations.add(donation);
            donation.setLinkedRequest(this);
        }
    }

    public void removeDonation(Donation donation) {
        if (donation != null && donations.remove(donation)) {
            donation.setLinkedRequest(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
        this.donations =
                donations != null ? donations : new ArrayList<>();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof DonationRequest other)) {
            return false;
        }

        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DonationRequest{" +
                "id=" + id +
                ", requestedBloodGroup=" + requestedBloodGroup +
                ", unitsNeeded=" + unitsNeeded +
                ", hospitalName='" + hospitalName + '\'' +
                ", hospitalAddress='" + hospitalAddress + '\'' +
                ", urgencyLevel=" + urgencyLevel +
                ", requestedBy=" +
                (requestedBy != null ? "<User reference>" : null) +
                ", status=" + status +
                ", requestDate=" + requestDate +
                ", notes='" + notes + '\'' +
                ", donations=" +
                (donations != null ? "<Donation collection>" : null) +
                '}';
    }
}