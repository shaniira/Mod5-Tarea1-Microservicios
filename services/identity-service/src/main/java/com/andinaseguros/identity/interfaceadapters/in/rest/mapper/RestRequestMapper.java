package com.andinaseguros.identity.interfaceadapters.in.rest.mapper;

import com.andinaseguros.identity.interfaceadapters.in.rest.request.*;
import com.andinaseguros.identity.usecases.dto.*;

public final class RestRequestMapper {
    private RestRequestMapper() {}

    public static LoginRequestModel toCore(LoginRequest value) {
        return new LoginRequestModel(value.username(), value.password());
    }

    public static CrearUsuarioRequestModel toCore(CrearUsuarioRequest value) {
        return new CrearUsuarioRequestModel(value.username(), value.password(), value.rol());
    }

    public static GoogleLoginRequestModel toCore(GoogleLoginRequest value) {
        return new GoogleLoginRequestModel(value.idToken());
    }

    public static MfaVerifyRequestModel toCore(MfaVerifyRequest value) {
        return new MfaVerifyRequestModel(value.challengeToken(), value.codigo());
    }
}
