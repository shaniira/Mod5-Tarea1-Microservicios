package com.andinaseguros.usecases.port.out.security;

public interface GoogleIdentityVerifierPort {
    GoogleIdentity verificar(String idToken);
}
