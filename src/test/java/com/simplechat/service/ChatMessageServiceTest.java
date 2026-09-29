package com.simplechat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.simplechat.domain.entity.ChatMessage;
import com.simplechat.domain.entity.ChatMessageStatus;
import com.simplechat.domain.entity.Conversation;
import com.simplechat.domain.entity.User;
import com.simplechat.domain.repository.ChatMessageRepository;
import com.simplechat.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ChatMessageServiceTest {

    @Mock
    UserService userService;

    @Mock
    ConversationService conversationService;

    @Mock
    ChatMessageRepository messageRepository;

    @InjectMocks
    ChatMessageService messageService;

    @Test
    void createsUndeliveredMessage() {
        User sender = getSender();
        User recipient = getRecipient();

        when(userService.getById(1L)).thenReturn(sender);
        when(userService.getById(2L)).thenReturn(recipient);
        when(conversationService.ensureConversation(any(User.class), any(User.class)))
            .thenAnswer(inv -> getConversation(inv.getArgument(0), inv.getArgument(1)));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage result = messageService.createMessage(1L, 2L, "message text", false);

        assertThat(result.getText()).isEqualTo("message text");
        assertThat(result.getStatus()).isEqualTo(ChatMessageStatus.SENT);
        assertThat(result.getSender().equals(sender));
        assertThat(result.getRecipient().equals(recipient));

        verify(userService).getById(1L);
        verify(userService).getById(2L);
        verify(conversationService).ensureConversation(sender, recipient);
    }

    @Test
    void createsDeliveredMessage() {
        User sender = getSender();
        User recipient = getRecipient();

        when(userService.getById(1L)).thenReturn(sender);
        when(userService.getById(2L)).thenReturn(recipient);
        when(conversationService.ensureConversation(any(User.class), any(User.class)))
            .thenAnswer(inv -> getConversation(inv.getArgument(0), inv.getArgument(1)));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        ChatMessage result = messageService.createMessage(1L, 2L, "message text", true);

        assertThat(result.getText()).isEqualTo("message text");
        assertThat(result.getStatus()).isEqualTo(ChatMessageStatus.DELIVERED);
        assertThat(result.getSender().equals(sender));
        assertThat(result.getRecipient().equals(recipient));

        verify(userService).getById(1L);
        verify(userService).getById(2L);
        verify(conversationService).ensureConversation(sender, recipient);
    }

    @Test
    void throwsOnChatWithYourself() {
        assertThatThrownBy(() -> messageService.createMessage(1L, 1L, "message text", true))
            .isInstanceOf(ApiException.class)
            .hasMessage("Cannot send a message to yourself");
    }

    User getSender() {
        User user = new User();
        user.setId(1L);
        user.setNickname("sender");
        user.setPasswordHash("SenderPasswordHash");
        return user;
    }

    User getRecipient() {
        User user = new User();
        user.setId(1L);
        user.setNickname("recipient");
        user.setPasswordHash("RecipientPasswordHash");
        return user;
    }

    Conversation getConversation(User userOne, User userTwo) {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setLowIdUser(userOne.getId() < userTwo.getId() ? userOne : userTwo);
        conversation.setHighIdUser(userOne.getId() > userTwo.getId() ? userOne : userTwo);
        return conversation;
    }

}
