package com.pxfintech.chat_service.repository;

import com.pxfintech.chat_service.model.UserConversation;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface UserConversationRepository
        extends CassandraRepository<UserConversation, UserConversation.UserConversationKey> {

    Slice<UserConversation> findByKeyUserId(UUID userId, Pageable pageable);

    @Query("SELECT * FROM user_conversations WHERE user_id = ?0 AND last_message_time < ?1")
    Slice<UserConversation> findByUserIdAndLastMessageTimeBefore(
            UUID userId, Instant lastMessageTime, Pageable pageable);

    @Query("UPDATE user_conversations SET unread_count = unread_count + 1 " +
            "WHERE user_id = ?0 AND conversation_id = ?1")
    void incrementUnreadCount(UUID userId, UUID conversationId);

    @Query("UPDATE user_conversations SET unread_count = 0, last_read_message_id = ?2, " +
            "last_read_time = ?3 WHERE user_id = ?0 AND conversation_id = ?1")
    void markAsRead(UUID userId, UUID conversationId, UUID lastReadMessageId, Instant lastReadTime);
}