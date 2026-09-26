package com.andinaseguros.entities.model;

import com.andinaseguros.entities.enums.TipoUso;
import com.andinaseguros.entities.enums.TipoVehiculo;
import com.andinaseguros.entities.valueobject.Placa;
import java.time.Year;
import java.util.UUID;

public class Vehiculo {
    private final UUID id;
    private final UUID clienteId;
    private final Placa placa;
    private final String marca;
    private final String modelo;
    private final int anioFabricacion;
    private final TipoVehiculo tipo;
    private final TipoUso uso;
    private final String zonaCirculacion;

    public Vehiculo(
            UUID id,
            UUID clienteId,
            Placa placa,
            String marca,
            String modelo,
            int anioFabricacion,
            TipoVehiculo tipo,
            TipoUso uso,
            String zonaCirculacion) {
        int antiguedad = Year.now().getValue() - anioFabricacion;
        if (antiguedad < 0 || antiguedad > 20)
            throw new IllegalArgumentException(
                    "La antigüedad del vehículo debe ser entre 0 y 20 años");
        this.id = id;
        this.clienteId = clienteId;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.anioFabricacion = anioFabricacion;
        this.tipo = tipo;
        this.uso = uso;
        this.zonaCirculacion = zonaCirculacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public Placa getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public int getAnioFabricacion() {
        return anioFabricacion;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public TipoUso getUso() {
        return uso;
    }

    public String getZonaCirculacion() {
        return zonaCirculacion;
    }

    public int antiguedad() {
        return Year.now().getValue() - anioFabricacion;
    }
}
