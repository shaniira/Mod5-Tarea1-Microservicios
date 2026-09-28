package com.backendseguros.customer.usecases.model;

public record VehicleInformation(
        String placa,
        String marca,
        String modelo,
        Integer anio,
        String tipoUso,
        String descripcion,
        String vin,
        String fuente) {}
