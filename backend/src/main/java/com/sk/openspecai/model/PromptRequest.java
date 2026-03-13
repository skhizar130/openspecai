
package com.sk.openspecai.model;

import jakarta.validation.constraints.NotBlank;

public record PromptRequest(

                @NotBlank(message = "name is required") String name,

                @NotBlank(message = "instruction is required") String instruction

) {
}
