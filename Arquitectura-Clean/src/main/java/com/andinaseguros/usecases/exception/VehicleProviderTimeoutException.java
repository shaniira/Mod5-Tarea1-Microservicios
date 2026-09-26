package com.andinaseguros.usecases.exception;

public class VehicleProviderTimeoutException extends VehicleProviderException {
    public VehicleProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
