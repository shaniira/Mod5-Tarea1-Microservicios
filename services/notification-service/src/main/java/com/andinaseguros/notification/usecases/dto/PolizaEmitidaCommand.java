package com.andinaseguros.notification.usecases.dto;

import java.util.UUID;

public record PolizaEmitidaCommand(UUID eventId, UUID polizaId, UUID clienteId, String numeroPoliza) {}
