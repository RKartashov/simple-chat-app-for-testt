package com.simplechat.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank @Size(min = 3, max = 32) @Pattern(regexp = "^[A-Za-z0-9_]+$") String nickname,
    @NotBlank @Size(min = 4, max = 72) String password
) {

}
