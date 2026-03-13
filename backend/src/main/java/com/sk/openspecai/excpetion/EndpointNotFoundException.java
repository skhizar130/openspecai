package com.sk.openspecai.excpetion;

public class EndpointNotFoundException extends RuntimeException {
    public EndpointNotFoundException(String message) {
        super(message);
    }
}
