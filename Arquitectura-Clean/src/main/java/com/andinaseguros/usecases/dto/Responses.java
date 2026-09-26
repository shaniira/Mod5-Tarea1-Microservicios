package com.andinaseguros.usecases.dto;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.model.ResultadoTarificacion;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class Responses {
    private Responses() {}

    public record ClienteResponse(
            UUID id,
            String tipoDocumento,
            String numeroDocumento,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String correo,
            String telefono,
            boolean activo) {}

    public record VehiculoResponse(
            UUID id,
            UUID clienteId,
            String placa,
            String marca,
            String modelo,
            int anioFabricacion,
            TipoVehiculo tipo,
            TipoUso uso,
            String zonaCirculacion) {}

    public record TablaResponse(
            UUID id,
            String codigo,
            int version,
            TipoVehiculo tipoVehiculo,
            TipoUso tipoUso,
            BigDecimal primaBase,
            BigDecimal primaMinima,
            LocalDate inicio,
            LocalDate fin,
            String notaTecnica,
            EstadoTablaTarifaria estado) {}

    public record FactorResponse(
            UUID id,
            String codigo,
            String nombre,
            String tipoVariable,
            BigDecimal valorMinimo,
            BigDecimal valorMaximo,
            BigDecimal multiplicador,
            int orden) {}

    public record TablaDetalleResponse(
            UUID id,
            String codigo,
            int version,
            TipoVehiculo tipoVehiculo,
            TipoUso tipoUso,
            BigDecimal primaBase,
            BigDecimal primaMinima,
            LocalDate inicio,
            LocalDate fin,
            String notaTecnica,
            EstadoTablaTarifaria estado,
            List<FactorResponse> factores) {}

    public record CotizacionResponse(
            UUID id,
            String numero,
            UUID clienteId,
            UUID vehiculoId,
            BigDecimal prima,
            String moneda,
            LocalDateTime creada,
            LocalDateTime expira,
            EstadoCotizacion estado,
            ResultadoTarificacion desglose) {}

    public record PolizaResponse(
            UUID id,
            String numero,
            UUID cotizacionId,
            UUID clienteId,
            UUID vehiculoId,
            BigDecimal prima,
            String moneda,
            LocalDate inicio,
            LocalDate fin,
            EstadoPoliza estado,
            UUID renovacionOrigenId) {}

    public record SiniestroResponse(
            UUID id,
            UUID polizaId,
            LocalDate fecha,
            String tipo,
            BigDecimal montoEstimado,
            boolean responsabilidadAsegurado,
            String gravedad,
            EstadoSiniestro estado) {}

    public record RenovacionResponse(
            UUID id,
            UUID polizaOrigenId,
            BigDecimal primaAnterior,
            BigDecimal nuevaPrima,
            BigDecimal porcentajeVariacion,
            int siniestrosConsiderados,
            EstadoRenovacion estado,
            String motivo,
            LocalDateTime creadaEn,
            LocalDateTime venceEn,
            LocalDateTime decididaEn,
            UUID polizaRenovadaId) {}

    public record TokenResponse(String token, String tipo, long expiraEnSegundos) {}

    public record ResultadoLogin(
            boolean requiresMfa,
            TokenResponse token,
            String challengeToken,
            long challengeExpiresIn) {
        public static ResultadoLogin exitoso(TokenResponse token) {
            return new ResultadoLogin(false, token, null, 0);
        }

        public static ResultadoLogin requiereMfa(String challengeToken, long expiresIn) {
            return new ResultadoLogin(true, null, challengeToken, expiresIn);
        }
    }

    public record MfaSetupResponse(String secret, String otpauthUri, String qrCodeDataUri) {}

    public record PerfilResponse(
            String username, String rol, String nombres, String apellidos, String email) {}

    public record MfaStatusResponse(boolean habilitado) {}

    public record PolizaConRenovacionesResponse(
            PolizaResponse poliza, List<RenovacionResponse> renovaciones) {}

    public record MiCuentaResponse(
            ClienteResponse cliente, List<PolizaConRenovacionesResponse> polizas) {}
}
