package com.andinaseguros.customer.usecases.exception;

public class VehicleProviderTimeoutException extends VehicleProviderException {
    public VehicleProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
