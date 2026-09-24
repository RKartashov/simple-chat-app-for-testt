package com.simplechat.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void issuesAndParsesToken() {
        var properties = new com.simplechat.config.JwtProperties(
                "change-me-please-use-a-long-secret-key-32b", Duration.ofHours(24));
        var jwtService = new JwtService(properties);

        var issued = jwtService.issueToken(7L, "roman");
        var claims = jwtService.parse(issued.token());

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("nickname", String.class)).isEqualTo("roman");
        assertThat(claims.getId()).isEqualTo(issued.jti());
    }
}
