package com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository;

import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.UsuarioDocument;
import java.util.*;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SpringDataUsuarioMongoRepository extends MongoRepository<UsuarioDocument, String> {
    Optional<UsuarioDocument> findByUsername(String username);

    Optional<UsuarioDocument> findByEmail(String email);

    Optional<UsuarioDocument> findByGoogleSubject(String googleSubject);

    Optional<UsuarioDocument> findByProviderAndProviderUserId(String provider, String providerUserId);
}
