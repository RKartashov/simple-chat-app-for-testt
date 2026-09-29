package com.simplechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simplechat.domain.entity.Conversation;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.ConversationRepository;
import com.simplechat.exception.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ConversationServiceTest {

    @Mock
    ConversationRepository conversationRepository;

    @InjectMocks
    ConversationService conversationService;

    @Test
    void ensuresExistingConversation() {
        when(conversationRepository.findFirstByLowIdUserIdAndHighIdUserId(1L, 2L))
            .thenReturn(Optional.of(getConversation()));

        Conversation result = conversationService.ensureConversation(getHighIdUser(), getLowIdUser());

        assertThat(result.equals(getConversation()));
        verify(conversationRepository).findFirstByLowIdUserIdAndHighIdUserId(1L, 2L);
    }

    @Test
    void createsNonExistingConversation() {
        when(conversationRepository.findFirstByLowIdUserIdAndHighIdUserId(1L, 2L))
            .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation convo = inv.getArgument(0);
            convo.setId(1L);
            return convo;
        });

        conversationService.ensureConversation(getHighIdUser(), getLowIdUser());

        verify(conversationRepository).findFirstByLowIdUserIdAndHighIdUserId(1L, 2L);
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void throwsOnCreateConversationWithSameUser() {
        assertThatThrownBy(() -> conversationService.ensureConversation(getLowIdUser(), getLowIdUser()))
            .isInstanceOf(ApiException.class)
            .hasMessage("Cannot create a conversation with yourself");

        verify(conversationRepository, never()).findFirstByLowIdUserIdAndHighIdUserId(any(), any());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getsConversationByUserIds() {
        when(conversationRepository.findFirstByLowIdUserIdAndHighIdUserId(1L, 2L))
            .thenReturn(Optional.of(getConversation()));

        Conversation result = conversationService.getByUserIds(2L, 1L);

        assertThat(result.equals(getConversation()));
    }

    @Test
    void throwsOnGettingNonexistentConversation() {
        when(conversationRepository.findFirstByLowIdUserIdAndHighIdUserId(1L, 2L))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.getByUserIds(2L, 1L))
            .isInstanceOf(ApiException.class)
            .hasMessage("Conversation between users with ids '1' and '2' not found");
    }

    Conversation getConversation() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setLowIdUser(getLowIdUser());
        conversation.setHighIdUser(getHighIdUser());
        return conversation;
    }

    User getLowIdUser() {
        User user = new User();
        user.setId(1L);
        user.setNickname("lowId");
        user.setPasswordHash("LowIdUserPasswordHash");
        return user;
    }

    User getHighIdUser() {
        User user = new User();
        user.setId(2L);
        user.setNickname("highId");
        user.setPasswordHash("HighIdUserPasswordHash");
        return user;
    }

}
