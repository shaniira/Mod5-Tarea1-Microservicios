package com.andinaseguros.customer.usecases.mapper;

import com.andinaseguros.customer.usecases.dto.Responses.ClienteResponse;
import com.andinaseguros.customer.entities.model.Cliente;

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
