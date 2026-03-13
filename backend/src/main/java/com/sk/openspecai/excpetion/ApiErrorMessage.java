package com.sk.openspecai.excpetion;

import java.time.Instant;

public record ApiErrorMessage(String error, String message, Instant timestamp) {

}
