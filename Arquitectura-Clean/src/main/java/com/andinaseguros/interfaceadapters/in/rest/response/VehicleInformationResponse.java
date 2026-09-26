package com.andinaseguros.interfaceadapters.in.rest.response;

import com.andinaseguros.usecases.model.VehicleInformation;

public record VehicleInformationResponse(
        String placa,
        String marca,
        String modelo,
        Integer anio,
        String tipoUso,
        String descripcion,
        String vin,
        String fuente,
        String mensaje) {
    public static VehicleInformationResponse from(VehicleInformation information) {
        String mensaje =
                "SIN_DATOS".equals(information.fuente())
                        ? "No se encontraron datos externos. Complete el registro manualmente."
                        : "Datos externos encontrados. Verifique antes de guardar.";
        return new VehicleInformationResponse(
                information.placa(),
                information.marca(),
                information.modelo(),
                information.anio(),
                information.tipoUso(),
                information.descripcion(),
                information.vin(),
                information.fuente(),
                mensaje);
    }
}
