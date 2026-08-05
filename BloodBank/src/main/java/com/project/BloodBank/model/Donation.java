package com.project.BloodBank.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.Objects;

// One recorded act of giving blood, entered by an administrator after the collection.
// A donation is a historical fact, so nothing here is edited or deleted once written.
@Entity
@Table(name="donations")
public class Donation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many donations point at one user, and the foreign key lives in this table. LAZY means the
    // User is a placeholder until something reads it, which must happen inside a transaction.
    //
    // ON DELETE RESTRICT makes the database refuse to delete a user who still has donations - which
    // is why accounts are deactivated rather than deleted.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "donor_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_donation_donor",
                    foreignKeyDefinition = "FOREIGN KEY (donor_id) REFERENCES users(id) ON DELETE RESTRICT"
            )
    )
    private User donor;

    // The day blood was collected, not the day this was typed in. A late entry carries an earlier
    // date, which is why DonationService only moves lastDonationDate forward.
    @Column(nullable=false)
    private LocalDate donationDate;

    @Column(nullable=false)
    private int unitsDonated;

    @Column
    private String location;

    // The approved request this was collected for, or null for a walk-in. Setting it marks the
    // request FULFILLED, which DonationService only allows once it has checked the request is still
    // approved and the blood is safe for that patient.
    //
    // SET NULL rather than RESTRICT here: if a request were ever removed, the donation still
    // happened and should survive unlinked rather than block the delete.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "linked_request_id",
            nullable = true,
            foreignKey = @ForeignKey(
                    name = "fk_donation_linked_request",
                    foreignKeyDefinition = "FOREIGN KEY (linked_request_id) REFERENCES donation_requests(id) ON DELETE SET NULL"
            )
    )
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

    // Same rule as the other entities: equal means the same row, and the null guard stops every
    // unsaved Donation comparing equal to every other unsaved one.
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
