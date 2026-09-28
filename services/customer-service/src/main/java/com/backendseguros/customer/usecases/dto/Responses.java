package com.backendseguros.customer.usecases.dto;

import com.backendseguros.customer.entities.enums.TipoUso;
import com.backendseguros.customer.entities.enums.TipoVehiculo;
import java.time.LocalDate;
import java.util.UUID;

/** Mismas respuestas que daba el monolito en /api/clientes (el frontend no cambia). */
public final class Responses {
    private Responses() {}

    public record ClienteResponse(
            UUID id,
            String tipoDocumento,
            String numeroDocumento,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String correo,
            String telefono,
            boolean activo) {}

    public record VehiculoResponse(
            UUID id,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int anioFabricacion,
            TipoVehiculo tipo,
            TipoUso uso,
            String zonaCirculacion) {}

    /** Resultado del backfill: cuántos customer.registered y vehicle.registered se publicaron. */
    public record ReenvioEventosResponse(int clientesPublicados, int vehiculosPublicados) {}
}
