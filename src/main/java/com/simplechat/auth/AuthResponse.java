package com.simplechat.auth;

import com.simplechat.user.UserDto;

public record AuthResponse(String token, UserDto user) {
}
