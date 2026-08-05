package com.project.BloodBank.model.enums;

// Where a request has got to.
//
// The path is one-way. A request arrives PENDING; approving or rejecting it is final, since the
// service refuses to decide anything that is not still PENDING. FULFILLED is never set by hand -
// it happens when an administrator records a donation against an approved request.
//
// So an APPROVED request is an unfinished job, and the app treats it as one: it stays visible with
// a "Find donors" action until a donation closes it.
public enum RequestStatus {
    PENDING, APPROVED, REJECTED, FULFILLED;
}
