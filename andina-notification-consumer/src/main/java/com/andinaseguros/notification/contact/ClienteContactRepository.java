package com.andinaseguros.notification.contact;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ClienteContactRepository extends MongoRepository<ClienteDocument, String> {}
