package com.andinaseguros.interfaceadapters.in.rest.mapper;

import com.andinaseguros.interfaceadapters.in.rest.request.*;
import com.andinaseguros.usecases.dto.*;
import java.util.List;

public final class RestRequestMapper {
    private RestRequestMapper() {}

    public static CrearClienteRequestModel toCore(CrearClienteRequest value) {
        return new CrearClienteRequestModel(value.tipoDocumento(), value.numeroDocumento(), value.nombres(), value.apellidos(), value.fechaNacimiento(), value.correo(), value.telefono());
    }

    public static CrearVehiculoRequestModel toCore(CrearVehiculoRequest value) {
        return new CrearVehiculoRequestModel(value.clienteId(), value.placa(), value.marca(), value.modelo(), value.anioFabricacion(), value.tipo(), value.uso(), value.zonaCirculacion());
    }

    public static CrearCotizacionRequestModel toCore(CrearCotizacionRequest value) {
        return new CrearCotizacionRequestModel(value.clienteId(), value.vehiculoId(), value.siniestrosResponsables(), value.porcentajeGastos(), value.porcentajeRecargo(), value.porcentajeDescuento());
    }

    public static EmitirPolizaRequestModel toCore(EmitirPolizaRequest value) {
        return new EmitirPolizaRequestModel(value.cotizacionId(), value.inicioVigencia());
    }

    public static RegistrarSiniestroRequestModel toCore(RegistrarSiniestroRequest value) {
        return new RegistrarSiniestroRequestModel(value.polizaId(), value.fecha(), value.tipo(), value.montoEstimado(), value.responsabilidadAsegurado(), value.gravedad(), value.estado());
    }

    public static CrearTablaRequestModel toCore(CrearTablaRequest value) {
        List<CrearFactorRequestModel> factores = value.factores() == null ? List.of() : value.factores().stream().map(RestRequestMapper::toCore).toList();
        return new CrearTablaRequestModel(value.codigo(), value.version(), value.tipoVehiculo(), value.tipoUso(), value.primaBase(), value.primaMinima(), value.inicioVigencia(), value.finVigencia(), value.codigoNotaTecnica(), value.estado(), factores);
    }

    private static CrearFactorRequestModel toCore(CrearFactorRequest value) {
        return new CrearFactorRequestModel(value.codigo(), value.nombre(), value.tipoVariable(), value.valorMinimo(), value.valorMaximo(), value.multiplicador(), value.orden());
    }

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
