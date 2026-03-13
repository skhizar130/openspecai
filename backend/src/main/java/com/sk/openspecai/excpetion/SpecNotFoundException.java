package com.sk.openspecai.excpetion;

public class SpecNotFoundException extends RuntimeException {
    public SpecNotFoundException(String message) {
        super(message);
    }
}
