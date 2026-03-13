package com.pxfintech.chat_service.repository;

import com.pxfintech.chat_service.model.Conversation;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationRepository extends CassandraRepository<Conversation, UUID> {

    @Query("SELECT * FROM conversations WHERE id = ?0")
    Conversation findById(UUID id);

    @Query("UPDATE conversations SET last_message_id = ?1, last_message_content = ?2, " +
            "last_message_time = ?3, last_message_sender = ?4 WHERE id = ?0")
    void updateLastMessage(UUID conversationId, UUID messageId, String content,
                           Instant time, UUID senderId);

    @Query("SELECT * FROM conversations WHERE id IN ?0")
    List<Conversation> findByIds(List<UUID> ids);
}