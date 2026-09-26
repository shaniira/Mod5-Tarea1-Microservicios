package com.andinaseguros.interfaceadapters.out.external.jsonpe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

record JsonPePlateRequest(String placa) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record JsonPePlateResponse(boolean success, String message, JsonPeVehicleData data) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record JsonPeVehicleData(
        String placa,
        String marca,
        String modelo,
        String serie,
        String color,
        String motor,
        String vin) {}
