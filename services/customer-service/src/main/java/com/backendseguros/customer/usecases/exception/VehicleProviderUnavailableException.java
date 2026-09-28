package com.backendseguros.customer.usecases.exception;

public class VehicleProviderUnavailableException extends VehicleProviderException {
    public VehicleProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
