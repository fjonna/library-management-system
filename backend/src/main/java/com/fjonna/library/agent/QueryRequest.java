package com.fjonna.library.agent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(@NotBlank @Size(max = 300) String question) {
}
