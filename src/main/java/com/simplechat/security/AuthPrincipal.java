package com.simplechat.security;

public record AuthPrincipal(Long id, String nickname, String tokenJti) {
}
