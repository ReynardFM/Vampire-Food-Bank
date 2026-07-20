package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.UrgencyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "donation_requests",
        indexes = {
                @Index(
                        name = "idx_request_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_request_date",
                        columnList = "request_date"
                ),
                @Index(
                        name = "idx_request_bloodgroup",
                        columnList = "requested_blood_group"
                )
        }
)
public class DonationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BloodGroup requestedBloodGroup;

    @Min(1)
    @Column(nullable = false)
    private int unitsNeeded;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String hospitalName;

    @Column()
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

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_id")
    private User processedBy;

    @Column()
    private LocalDateTime processedDate;

    @CreationTimestamp
    @Column(
            updatable = false
    )
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

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

        if (processedDate == null
                && (status == RequestStatus.APPROVED
                || status == RequestStatus.REJECTED)) {
            processedDate = LocalDateTime.now();
        }
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

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
        return id != null ? id.hashCode() : 0;
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
                ", processedBy=" +
                (processedBy != null ? "<User reference>" : null) +
                ", processedDate=" + processedDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", donations=" +
                (donations != null ? "<Donation collection>" : null) +
                '}';
    }
}