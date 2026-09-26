package com.andinaseguros.usecases.exception;

public abstract class VehicleProviderException extends RuntimeException {
    protected VehicleProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
