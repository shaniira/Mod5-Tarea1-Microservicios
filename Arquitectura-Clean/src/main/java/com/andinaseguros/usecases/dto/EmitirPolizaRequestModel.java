package com.andinaseguros.usecases.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EmitirPolizaRequestModel(UUID cotizacionId, LocalDate inicioVigencia) {}
