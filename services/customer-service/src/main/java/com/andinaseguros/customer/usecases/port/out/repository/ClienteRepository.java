package com.andinaseguros.customer.usecases.port.out.repository;

import com.andinaseguros.customer.entities.model.Cliente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {
    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(UUID id);

    Optional<Cliente> buscarPorDocumento(String documento);

    Optional<Cliente> buscarPorCorreo(String correo);

    List<Cliente> listar();
}
