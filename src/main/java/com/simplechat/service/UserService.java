package com.simplechat.service;

import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.UserRepository;
import com.simplechat.exception.ApiException;
import com.simplechat.rest.dto.UserDto;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.websocket.WebSocketSessionRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        String needle = query == null ? "" : query.trim();
        return userRepository.findAllByIdNotAndNicknameContainingIgnoreCase(current.id(), needle)
                             .stream()
                             .map(this::toDto)
                             .sorted(
                                 Comparator.comparing(UserDto::online)
                                           .reversed()
                                           .thenComparing(UserDto::nickname, String.CASE_INSENSITIVE_ORDER)
                             )
                             .toList();
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(AuthPrincipal current) {
        User user = getById(current.id());
        return toDto(user);
    }

    public UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getNickname(), sessionRegistry.isOnline(user.getId()));
    }

    public Optional<User> findById(Long userId) {
        return userRepository.findById(userId);
    }

    public User getById(Long userId) {
        return findById(userId).orElseThrow(() -> getUserNotFoundException(userId));
    }

    public boolean isUserExistsById(Long userId) {
        return userRepository.existsById(userId);
    }

    public ApiException getUserNotFoundException(Long userId) {
        return new ApiException(HttpStatus.NOT_FOUND.value(), String.format("User with id %s not found", userId));
    }

}
