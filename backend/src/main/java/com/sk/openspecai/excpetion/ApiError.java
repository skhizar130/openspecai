package com.sk.openspecai.excpetion;

import java.time.Instant;

public record ApiError(String error, Instant timestamp) {

}
