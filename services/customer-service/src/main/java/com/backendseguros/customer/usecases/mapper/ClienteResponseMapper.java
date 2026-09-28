package com.backendseguros.customer.usecases.mapper;

import com.backendseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.backendseguros.customer.entities.model.Cliente;

public final class ClienteResponseMapper {
    private ClienteResponseMapper() {}

    public static ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getTipoDocumento(),
                cliente.getNumeroDocumento(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getFechaNacimiento(),
                cliente.getCorreo(),
                cliente.getTelefono(),
                cliente.isActivo());
    }
}
