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
}
