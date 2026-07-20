package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import com.project.BloodBank.model.enums.Role;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users",
        indexes = {
        @Index(name = "idx_user_bloodgroup", columnList = "blood_group"),
                @Index(name = "idx_user_email", columnList = "email") })

public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullname;

    @Column(nullable = false, unique = true)
    private String password;

    @Column
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private BloodGroup bloodGroup;

    @Column
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private LocalDate lastDonationDate;

    @Column(nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "requestedBy", fetch = FetchType.LAZY)
    private List<DonationRequest> donationRequests = new ArrayList<>();

    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    User(){}



}
