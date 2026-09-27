package com.simplechat.service;

import com.simplechat.common.EntityService;
import com.simplechat.domain.entity.Conversation;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.ConversationRepository;
import com.simplechat.exception.ApiException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationService implements EntityService<Conversation, Long> {

    @Getter
    private final ConversationRepository repository;

    public Conversation ensureConversation(User userOne, User userTwo) {
        User lowIdUser = userOne.getId() < userTwo.getId() ? userOne : userTwo;
        User highIdUser = lowIdUser == userOne ? userTwo : userOne;

        return repository.findFirstByLowIdUserIdAndHighIdUserId(lowIdUser.getId(), highIdUser.getId())
                         .orElseGet(() -> createConversation(lowIdUser, highIdUser));
    }

    public Conversation getByUserIds(Long userOneId, Long userTwoId) {
        Long lowId = Math.min(userOneId, userTwoId);
        Long highId = Math.max(userOneId, userTwoId);

        return repository.findFirstByLowIdUserIdAndHighIdUserId(lowId, highId)
                         .orElseThrow(() -> new ApiException(
                             HttpStatus.NOT_FOUND.value(),
                             String.format("Conversation between users with ids '%s' and '%s' not found", lowId, highId)
                         ));
    }

    private Conversation createConversation(User lowIdUser, User highIdUser) {
        Conversation conversation = new Conversation();
        conversation.setLowIdUser(lowIdUser);
        conversation.setHighIdUser(highIdUser);

        return save(conversation);
    }

}
