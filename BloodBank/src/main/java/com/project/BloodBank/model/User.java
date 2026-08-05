package com.project.BloodBank.model;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import com.project.BloodBank.model.enums.Role;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// An account, donor or administrator.
//
// This wears two hats: a JPA entity mapped to the "users" table, and the Spring Security
// UserDetails representing whoever is signed in - hence getUsername() and isEnabled() at the
// bottom. Convenient, but the principal is captured at sign-in and cached in the session, so it
// goes stale when the row changes. CurrentUserAdvice reloads it per request for that reason.
//
// Named "users" because USER is a reserved word in several databases.
@Entity
@Table(name = "users")
public class User implements UserDetails {

    // --- Identity ---
    // IDENTITY means the database auto-increments it, so this stays null until the row is saved.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Credentials ---
    // email is also the username. unique lets the database reject duplicates, which UserService's
    // check alone cannot: two simultaneous registrations both pass it, only the constraint stops
    // the second. password is always a BCrypt hash, never the typed value.
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // --- Profile ---
    // All optional, because registration only asks for name, email and password. A null bloodGroup
    // keeps the donor out of search and out of any request link, since nothing is known about who
    // they could give to.
    //
    // @Enumerated(STRING) stores the constant's name. The default, ORDINAL, stores its position -
    // compact, but it corrupts every existing row the moment anybody reorders the enum.
    @Column(nullable = false)
    private String fullName;

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

    // --- Account state ---
    // lastDonationDate duplicates what the donations list already implies, but is stored so listing
    // donors does not need a query per row. DonationService only ever moves it forward.
    //
    // active is a soft delete. A real delete is impossible here: donations and requests point at
    // this row under ON DELETE RESTRICT, so it would either fail or destroy history. This is also
    // what isEnabled() returns, so clearing it blocks sign-in too.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private LocalDate lastDonationDate;

    @Column(nullable = false)
    private boolean active;

    // --- Relationships ---
    // mappedBy says this side owns nothing: the foreign key lives in the other table. LAZY means
    // these are not loaded with the user, so they must only be read inside a transaction.
    @OneToMany(mappedBy = "requestedBy", fetch = FetchType.LAZY)
    private List<DonationRequest> donationRequests = new ArrayList<>();

    @OneToMany(mappedBy = "donor", fetch = FetchType.LAZY)
    private List<Donation> donations = new ArrayList<>();

    public User(){}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public BloodGroup getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<DonationRequest> getDonationRequests() {
        return donationRequests;
    }

    public void setDonationRequests(List<DonationRequest> donationRequests) {
        this.donationRequests = donationRequests;
    }

    public List<Donation> getDonations() {
        return donations;
    }

    public void setDonations(List<Donation> donations) {
        this.donations = donations;
    }

    // --- Spring Security contract ---
    // Everything below exists because of UserDetails, and is what lets this entity be the
    // signed-in principal directly.

    @Override
    public String getUsername() {
        return email;
    }

    // Spring Security has no notion of "role" beyond a naming convention: an authority starting
    // with "ROLE_" is treated as one. The prefix is added here, hence hasAuthority("ROLE_ADMIN").
    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // No expiry or lockout is modelled, so these three are always fine. Returning false from any
    // would block sign-in, so they cannot be left out.

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Deactivated accounts are refused at sign-in without a check anywhere else.
    @Override
    public boolean isEnabled() {
        return active;
    }

    // Equal means the same row. The null guard matters: without it every unsaved User would compare
    // equal to every other unsaved one, which quietly loses objects inside collections.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    // Matches equals(), with one consequence worth knowing: saving an entity changes its hash, so
    // anything put in a HashSet before saving is effectively lost afterwards.
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", bloodGroup=" + bloodGroup +
                ", dateOfBirth=" + dateOfBirth +
                ", gender=" + gender +
                ", address='" + address + '\'' +
                ", role=" + role +
                ", lastDonationDate=" + lastDonationDate +
                ", active=" + active +
                '}';
    }

}
