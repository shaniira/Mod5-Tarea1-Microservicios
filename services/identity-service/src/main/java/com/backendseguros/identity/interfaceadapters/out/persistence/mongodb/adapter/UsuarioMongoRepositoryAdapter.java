package com.backendseguros.identity.interfaceadapters.out.persistence.mongodb.adapter;

import com.backendseguros.identity.entities.model.Usuario;
import com.backendseguros.identity.usecases.port.out.repository.UsuarioRepository;
import com.backendseguros.identity.interfaceadapters.out.persistence.mongodb.mapper.UsuarioMongoMapper;
import com.backendseguros.identity.interfaceadapters.out.persistence.mongodb.repository.SpringDataUsuarioMongoRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioMongoRepositoryAdapter implements UsuarioRepository {
    private final SpringDataUsuarioMongoRepository repo;
    private final UsuarioMongoMapper mapper;

    public UsuarioMongoRepositoryAdapter(SpringDataUsuarioMongoRepository r, UsuarioMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public Usuario guardar(Usuario x) {
        return mapper.toDomain(repo.save(mapper.toDocument(x)));
    }

    public Optional<Usuario> buscarPorUsername(String x) {
        return repo.findByUsername(x).map(mapper::toDomain);
    }

    public Optional<Usuario> buscarPorEmail(String x) {
        return repo.findByEmail(x).map(mapper::toDomain);
    }

    public Optional<Usuario> buscarPorGoogleSubject(String x) {
        return repo.findByGoogleSubject(x).map(mapper::toDomain);
    }

    public Optional<Usuario> buscarPorProveedorYProveedorUsuarioId(String provider, String providerUserId) {
        return repo.findByProviderAndProviderUserId(provider, providerUserId).map(mapper::toDomain);
    }
}
