package com.project.BloodBank.exception;

// Thrown when something is looked up by id and is not there - a deleted request, a deactivated
// account, or simply a number typed into the URL.
//
// Extends RuntimeException rather than Exception so it is unchecked: no method has to declare it,
// and no caller is forced to catch it. That suits an error nearly every caller wants to let
// through, since GlobalExceptionHandler turns it into a 404 page in one place.
public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException (String message){
        super(message);
    }
}
