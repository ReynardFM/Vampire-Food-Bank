package com.project.BloodBank.exception;

// Thrown when registration is attempted with an address that already has an account.
//
// A dedicated type rather than a generic exception, so the caller can tell this apart from a real
// failure. AuthController catches it and attaches the message to the email field, which is why the
// error appears beside the input rather than at the top of the form.
public class EmailAlreadyExistsException extends RuntimeException{
    public EmailAlreadyExistsException(String message){
        super(message);
    }
}
