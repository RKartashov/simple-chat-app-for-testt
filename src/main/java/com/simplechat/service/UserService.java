package com.simplechat.service;

import com.simplechat.common.EntityService;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.UserRepository;
import com.simplechat.rest.dto.UserDto;
import com.simplechat.security.AuthPrincipal;
import com.simplechat.websocket.WebSocketSessionRegistry;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements EntityService<User, Long> {

    @Getter
    private final UserRepository repository;
    private final WebSocketSessionRegistry sessionRegistry;

    public User createUser(String nickname, String passwordHash) {
        User user = new User();
        user.setNickname(nickname);
        user.setPasswordHash(passwordHash);

        return save(user);
    }

    @Transactional(readOnly = true)
    public List<UserDto> getOtherUsers(AuthPrincipal current, String query) {
        String needle = query == null ? "" : query.trim();

        return repository.findAllByIdNotAndNicknameContainingIgnoreCase(current.id(), needle)
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
    public UserDto getCurrentUser(AuthPrincipal current) {
        User user = getById(current.id());

        return toDto(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByNickname(String nickname) {
        return repository.findByNicknameIgnoreCase(nickname);
    }

    @Transactional(readOnly = true)
    public boolean isUserExistsById(Long userId) {
        return repository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public boolean existsByNickname(String nickname) {
        return repository.existsByNicknameIgnoreCase(nickname);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getNickname(), sessionRegistry.isUserOnline(user.getId()));
    }

}
