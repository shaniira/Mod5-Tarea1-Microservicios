package com.andinaseguros.customer.usecases.dto;

import java.util.UUID;

public record ActualizarContactoClienteRequestModel(UUID clienteId, String correo, String telefono) {}
