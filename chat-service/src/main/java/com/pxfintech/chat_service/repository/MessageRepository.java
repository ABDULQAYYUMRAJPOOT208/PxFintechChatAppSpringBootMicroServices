package com.pxfintech.chat_service.repository;

import com.pxfintech.chat_service.model.Message;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface MessageRepository extends CassandraRepository<Message, Message.MessageKey> {

    Slice<Message> findByKeyConversationIdAndKeyCreatedAtLessThan(
            UUID conversationId,
            Instant createdAt,
            Pageable pageable);

    @Query("SELECT * FROM messages WHERE conversation_id = ?0 AND created_at < ?1 LIMIT ?2")
    Slice<Message> findRecentMessages(UUID conversationId, Instant before, int limit);

    @Query("SELECT COUNT(*) FROM messages WHERE conversation_id = ?0 AND created_at > ?1")
    long countNewMessages(UUID conversationId, Instant since);

    @Query("UPDATE messages SET status = 'DELIVERED' WHERE conversation_id = ?0 AND message_id = ?1")
    void markAsDelivered(UUID conversationId, UUID messageId);

    @Query("UPDATE messages SET status = 'READ' WHERE conversation_id = ?0 AND message_id = ?1")
    void markAsRead(UUID conversationId, UUID messageId);

    @Query("UPDATE messages SET deleted_for = deleted_for + {?2} WHERE conversation_id = ?0 AND message_id = ?1")
    void deleteForUser(UUID conversationId, UUID messageId, UUID userId);
}