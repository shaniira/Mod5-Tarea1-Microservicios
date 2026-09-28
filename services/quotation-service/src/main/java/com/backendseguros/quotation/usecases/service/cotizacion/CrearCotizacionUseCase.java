package com.backendseguros.quotation.usecases.service.cotizacion;

import static com.backendseguros.quotation.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.backendseguros.quotation.entities.enums.EstadoCotizacion;
import com.backendseguros.quotation.entities.exception.RecursoNoEncontradoException;
import com.backendseguros.quotation.entities.exception.ReglaNegocioException;
import com.backendseguros.quotation.entities.model.ClienteRef;
import com.backendseguros.quotation.entities.model.Cotizacion;
import com.backendseguros.quotation.entities.model.ResultadoTarificacion;
import com.backendseguros.quotation.entities.model.TablaTarifaria;
import com.backendseguros.quotation.entities.model.VehiculoRef;
import com.backendseguros.quotation.entities.service.MotorDeTarificacion;
import com.backendseguros.quotation.usecases.dto.CrearCotizacionRequestModel;
import com.backendseguros.quotation.usecases.dto.Responses.CotizacionResponse;
import com.backendseguros.quotation.usecases.port.in.CrearCotizacionInputPort;
import com.backendseguros.quotation.usecases.port.out.cliente.DirectorioClientesPort;
import com.backendseguros.quotation.usecases.port.out.repository.CotizacionRepository;
import com.backendseguros.quotation.usecases.port.out.repository.ReferenciaClientesRepository;
import com.backendseguros.quotation.usecases.port.out.repository.TablaTarifariaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Mismo flujo que el monolito (cliente, vehículo, propietario, tabla vigente, motor, cotización de
 * 15 días), pero el cliente y el vehículo salen de las proyecciones customer_ref y vehicle_ref
 * (paso 5.2). Si la proyección no los tiene (o le falta la fecha de nacimiento), se consulta a
 * customer-service (paso 5.3); si no responde, ClientesNoDisponiblesException (503). Así se puede
 * cotizar con customer-service caído cuando los datos ya están copiados.
 */
public class CrearCotizacionUseCase implements CrearCotizacionInputPort {
    /** Una referencia traída por la lectura de refuerzo se guarda con versión 0: cualquier evento la reemplaza. */
    static final long VERSION_LECTURA_REFUERZO = 0;

    private final ReferenciaClientesRepository referencias;
    private final DirectorioClientesPort directorio;
    private final TablaTarifariaRepository tablaTarifariaRepository;
    private final CotizacionRepository cotizacionRepository;
    private final MotorDeTarificacion motorDeTarificacion;

    public CrearCotizacionUseCase(
            ReferenciaClientesRepository referencias,
            DirectorioClientesPort directorio,
            TablaTarifariaRepository tablaTarifariaRepository,
            CotizacionRepository cotizacionRepository,
            MotorDeTarificacion motorDeTarificacion) {
        this.referencias = referencias;
        this.directorio = directorio;
        this.tablaTarifariaRepository = tablaTarifariaRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.motorDeTarificacion = motorDeTarificacion;
    }

    @Override
    public CotizacionResponse execute(CrearCotizacionRequestModel solicitud) {
        ClienteRef cliente = obtenerCliente(solicitud.clienteId());
        VehiculoRef vehiculo = obtenerVehiculo(solicitud.clienteId(), solicitud.vehiculoId());

        validarPropietario(cliente, vehiculo);

        TablaTarifaria tablaTarifaria = obtenerTablaTarifaria(vehiculo);
        ResultadoTarificacion resultado =
                motorDeTarificacion.calcular(
                        tablaTarifaria,
                        cliente,
                        vehiculo,
                        solicitud.siniestrosResponsables(),
                        valorOInicial(solicitud.porcentajeGastos(), "0.10"),
                        valorOInicial(solicitud.porcentajeRecargo(), "0.03"),
                        valorOInicial(solicitud.porcentajeDescuento(), "0.00"));

        LocalDateTime fechaCreacion = LocalDateTime.now();
        Cotizacion cotizacion =
                new Cotizacion(
                        UUID.randomUUID(),
                        "COT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                        solicitud.clienteId(),
                        solicitud.vehiculoId(),
                        tablaTarifaria.getId(),
                        resultado.primaComercial(),
                        fechaCreacion,
                        fechaCreacion.plusDays(15),
                        EstadoCotizacion.VIGENTE,
                        resultado);

        return toResponse(cotizacionRepository.guardar(cotizacion), resultado);
    }

    private ClienteRef obtenerCliente(UUID clienteId) {
        Optional<ClienteRef> local = referencias.buscarCliente(clienteId);
        if (local.isPresent() && local.get().completo()) {
            return local.get();
        }
        ClienteRef remoto =
                directorio
                        .buscarCliente(clienteId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Cliente"));
        if (local.isPresent()) {
            // Llegó por un evento del backend, sin fecha de nacimiento: solo se completa ese dato.
            referencias.completarFechaNacimiento(remoto);
        } else {
            referencias.guardarClienteSiEsMasNuevo(remoto, VERSION_LECTURA_REFUERZO);
        }
        return remoto;
    }

    private VehiculoRef obtenerVehiculo(UUID clienteId, UUID vehiculoId) {
        Optional<VehiculoRef> local = referencias.buscarVehiculo(vehiculoId);
        if (local.isPresent()) {
            return local.get();
        }
        VehiculoRef remoto =
                directorio
                        .buscarVehiculo(clienteId, vehiculoId)
                        .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo"));
        referencias.guardarVehiculoSiEsMasNuevo(remoto, VERSION_LECTURA_REFUERZO);
        return remoto;
    }

    private void validarPropietario(ClienteRef cliente, VehiculoRef vehiculo) {
        if (!cliente.clienteId().equals(vehiculo.clienteId())) {
            throw new ReglaNegocioException(
                    "VEHICULO_NO_PERTENECE", "El vehículo no pertenece al cliente");
        }
    }

    private TablaTarifaria obtenerTablaTarifaria(VehiculoRef vehiculo) {
        return tablaTarifariaRepository
                .buscarVigente(vehiculo.tipo(), vehiculo.uso(), LocalDate.now())
                .orElseThrow(
                        () ->
                                new ReglaNegocioException(
                                        "TABLA_NO_DISPONIBLE", "No existe tabla tarifaria vigente"));
    }

    private BigDecimal valorOInicial(BigDecimal valor, String valorInicial) {
        return valor == null ? new BigDecimal(valorInicial) : valor;
    }
}
