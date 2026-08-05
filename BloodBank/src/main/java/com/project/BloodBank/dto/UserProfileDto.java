package com.project.BloodBank.dto;

import com.project.BloodBank.model.enums.BloodGroup;
import com.project.BloodBank.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// What the profile form is allowed to change.
//
// Note what is missing: email, password, role and active. A donor editing their profile cannot
// reach any of them, because there is nowhere on this class to put them.
//
// All six are required here even though the matching columns on User are nullable. The database
// allows a half-filled profile, since registration creates one; the form does not, because a donor
// filling it in should finish the job.
public class UserProfileDto {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    // @NotNull rather than @NotBlank because this is an enum, not text - there is no blank enum.
    @NotNull(message = "Blood group is required")
    private BloodGroup bloodGroup;

    // @DateTimeFormat controls how the date is written into the form and read back out.
    //
    // Without it Spring uses a locale-dependent short format such as 8/3/26. An <input type="date">
    // only accepts yyyy-MM-dd, so the browser silently discarded the value and showed an empty
    // field - even though the date was stored perfectly well.
    @NotNull(message = "Date of birth is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotBlank(message = "Address is required")
    private String address;

    public UserProfileDto() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
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
}
