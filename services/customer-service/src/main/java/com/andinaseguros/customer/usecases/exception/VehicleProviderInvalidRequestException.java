package com.andinaseguros.customer.usecases.exception;

public class VehicleProviderInvalidRequestException extends VehicleProviderException {
    public VehicleProviderInvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
