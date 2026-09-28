package com.backendseguros.customer.interfaceadapters.out.persistence.mongodb.adapter;

import com.backendseguros.customer.entities.model.Cliente;
import com.backendseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.backendseguros.customer.interfaceadapters.out.persistence.mongodb.mapper.ClienteMongoMapper;
import com.backendseguros.customer.interfaceadapters.out.persistence.mongodb.repository.SpringDataClienteMongoRepository;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteMongoRepositoryAdapter implements ClienteRepository {
    private final SpringDataClienteMongoRepository repo;
    private final ClienteMongoMapper mapper;

    public ClienteMongoRepositoryAdapter(SpringDataClienteMongoRepository r, ClienteMongoMapper m) {
        repo = r;
        mapper = m;
    }

    public Cliente guardar(Cliente x) {
        return mapper.toDomain(repo.save(mapper.toDocument(x)));
    }

    public Optional<Cliente> buscarPorId(UUID id) {
        return repo.findById(id.toString()).map(mapper::toDomain);
    }

    public Optional<Cliente> buscarPorDocumento(String x) {
        return repo.findByNumeroDocumento(x).map(mapper::toDomain);
    }

    public Optional<Cliente> buscarPorCorreo(String x) {
        return repo.findByCorreo(x).map(mapper::toDomain);
    }

    public List<Cliente> listar() {
        return repo.findAll().stream().map(mapper::toDomain).toList();
    }
}
