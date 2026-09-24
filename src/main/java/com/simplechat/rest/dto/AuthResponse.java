package com.simplechat.rest.dto;

public record AuthResponse(String token, UserDto user) {
}
