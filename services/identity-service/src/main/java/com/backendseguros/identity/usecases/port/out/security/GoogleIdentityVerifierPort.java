package com.backendseguros.identity.usecases.port.out.security;

public interface GoogleIdentityVerifierPort {
    GoogleIdentity verificar(String idToken);
}
