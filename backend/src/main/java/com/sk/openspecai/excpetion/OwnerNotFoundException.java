package com.sk.openspecai.excpetion;

public class OwnerNotFoundException extends RuntimeException {
    public OwnerNotFoundException(String message) {
        super(message);
    }
}
