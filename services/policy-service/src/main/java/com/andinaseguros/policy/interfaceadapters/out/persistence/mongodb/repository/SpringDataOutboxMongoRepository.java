package com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.policy.interfaceadapters.out.persistence.mongodb.document.OutboxEventDocument;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataOutboxMongoRepository
        extends MongoRepository<OutboxEventDocument, String> {
    List<OutboxEventDocument> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    Optional<OutboxEventDocument> findFirstByStatusOrderByCreatedAtAsc(String status);

    long countByStatus(String status);
}
