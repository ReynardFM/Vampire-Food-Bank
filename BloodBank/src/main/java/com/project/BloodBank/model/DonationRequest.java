package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.RequestStatus;
import com.project.BloodBank.model.enums.UrgencyLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// A plea for blood: a group, a number of units, a hospital.
//
// The centre of the application. A donor raises one, it queues as PENDING, an administrator
// approves or rejects it. Approving does not finish the job - the request waits until a donation is
// recorded against it and becomes FULFILLED.
//
// Two fields here are not what their names suggest: urgencySeverity is about sorting, decidedAt is
// about knowing when the status last moved. Both are explained where they are declared.
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

    @Column()
    private String hospitalAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UrgencyLevel urgencyLevel;

    // The urgency level as a number, so the admin queue can sort by it. urgencyLevel is stored as
    // text, and sorting text gives CRITICAL, HIGH, LOW, MEDIUM - alphabetical, so LOW lands in the
    // middle and the column is useless.
    //
    // Storing the same thing twice usually means the copies drift. That is prevented by never
    // letting anything set this directly: there is no setter, setUrgencyLevel() maintains it, and
    // the lifecycle hook recalculates it before every save.
    @Column(nullable = false)
    private int urgencySeverity;

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

    // When the request arrived. updatable = false leaves it out of every UPDATE, so even a bug
    // assigning a new value could not overwrite what is stored.
    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestDate;

    // When the status last moved off PENDING. Needed because requestDate answers a different
    // question: a request raised three weeks ago but approved this morning is today's work.
    //
    // Anything past PENDING with a null here predates this column. Those are left out of the daily
    // figures rather than guessed at, since there is no way to know when they were decided.
    @Column
    private LocalDateTime decidedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;


    @OneToMany(
            mappedBy = "linkedRequest",
            fetch = FetchType.LAZY
    )
    private List<Donation> donations = new ArrayList<>();

    public DonationRequest() {
    }

    // Fills in what should not be left to whoever built the object. @PrePersist runs before the
    // first INSERT, @PreUpdate before every UPDATE, so this applies no matter who saves - including
    // the seeder and the tests.
    //
    // The first two checks are "only if not already set", so a caller that supplied a real value
    // keeps it. The seeder relies on that to backdate its requests.
    @PrePersist
    @PreUpdate
    public void setDefaultValues() {
        if (status == null) {
            status = RequestStatus.PENDING;
        }

        if (requestDate == null) {
            requestDate = LocalDateTime.now();
        }

        // Recalculated unconditionally, unlike the two above: this is a derived copy, not a choice,
        // so the right behaviour is always to overwrite it from its source.
        if (urgencyLevel != null) {
            urgencySeverity = urgencyLevel.getSeverity();
        }
    }

    // Sets both ends of the relationship at once. A two-way relationship in JPA is really two
    // fields describing the same thing, and nothing updates one when you change the other - setting
    // only the list would look right in memory and vanish on reload.
    public void addDonation(Donation donation) {
        if (donation != null && !donations.contains(donation)) {
            donations.add(donation);
            donation.setLinkedRequest(this);
        }
    }

    // The mirror image: clears both ends rather than just the list.
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

    // Does more than assign: it also refreshes the numeric copy used for sorting.
    public void setUrgencyLevel(UrgencyLevel urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
        this.urgencySeverity = urgencyLevel != null ? urgencyLevel.getSeverity() : 0;
    }

    // No matching setter on purpose. This follows the urgency level; letting anything set it
    // independently is how the two would fall out of step.
    public int getUrgencySeverity() {
        return urgencySeverity;
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

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
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

    // The relationship fields print as placeholders. Expanding them would fire a lazy query - or
    // throw, since toString() is usually called outside a transaction - and printing requestedBy
    // could recurse back through the User forever.
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