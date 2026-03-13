package com.sk.openspecai.model;

import jakarta.validation.constraints.NotBlank;

public record UpdateRequest(@NotBlank String instruction) {

}
