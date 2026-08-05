package com.project.BloodBank.model.enums;

// A donor's gender. An enum rather than free text so the profile form can offer fixed choices.
// Stored but not acted on - real blood services vary donation intervals by it, so it is kept for
// the record.
public enum Gender {
    MALE, FEMALE, OTHER
}
