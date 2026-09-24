package com.simplechat.user;

import com.simplechat.exception.ApiException;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.websocket.WebSocketSessionRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WebSocketSessionRegistry sessionRegistry;

    @Transactional(readOnly = true)
    public List<UserDto> listOthers(AuthPrincipal current, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(current.id()))
                .filter(user -> needle.isEmpty()
                        || user.getNickname().toLowerCase(Locale.ROOT).contains(needle))
                .map(this::toDto)
                .sorted(Comparator.comparing(UserDto::online)
                        .reversed()
                        .thenComparing(UserDto::nickname, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(AuthPrincipal current) {
        User user = userRepository
                .findById(current.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND.value(), "User not found"));
        return toDto(user);
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getNickname(), sessionRegistry.isOnline(user.getId()));
    }
}
