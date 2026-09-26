package com.andinaseguros.usecases.port.out.repository;

import com.andinaseguros.entities.model.Cliente;
import java.util.*;

public interface ClienteRepository {
    Cliente guardar(Cliente cliente);

    Optional<Cliente> buscarPorId(UUID id);

    Optional<Cliente> buscarPorDocumento(String doc);

    Optional<Cliente> buscarPorCorreo(String correo);

    List<Cliente> listar();
}
