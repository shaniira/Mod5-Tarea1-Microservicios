package com.andinaseguros.usecases.service.cotizacion;

import static com.andinaseguros.usecases.mapper.CotizacionResponseMapper.toResponse;

import com.andinaseguros.usecases.dto.CrearCotizacionRequestModel;
import com.andinaseguros.usecases.dto.Responses.CotizacionResponse;
import com.andinaseguros.usecases.port.in.CrearCotizacionInputPort;
import com.andinaseguros.entities.enums.EstadoCotizacion;
import com.andinaseguros.entities.exception.RecursoNoEncontradoException;
import com.andinaseguros.entities.exception.ReglaNegocioException;
import com.andinaseguros.entities.model.Cliente;
import com.andinaseguros.entities.model.Cotizacion;
import com.andinaseguros.entities.model.ResultadoTarificacion;
import com.andinaseguros.entities.model.TablaTarifaria;
import com.andinaseguros.entities.model.Vehiculo;
import com.andinaseguros.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.usecases.port.out.repository.CotizacionRepository;
import com.andinaseguros.usecases.port.out.repository.TablaTarifariaRepository;
import com.andinaseguros.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.entities.service.MotorDeTarificacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class CrearCotizacionUseCase implements CrearCotizacionInputPort {

    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final TablaTarifariaRepository tablaTarifariaRepository;
    private final CotizacionRepository cotizacionRepository;
    private final MotorDeTarificacion motorDeTarificacion;

    public CrearCotizacionUseCase(
            ClienteRepository clienteRepository,
            VehiculoRepository vehiculoRepository,
            TablaTarifariaRepository tablaTarifariaRepository,
            CotizacionRepository cotizacionRepository,
            MotorDeTarificacion motorDeTarificacion) {
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.tablaTarifariaRepository = tablaTarifariaRepository;
        this.cotizacionRepository = cotizacionRepository;
        this.motorDeTarificacion = motorDeTarificacion;
    }

    @Override
    public CotizacionResponse execute(CrearCotizacionRequestModel solicitud) {
        Cliente cliente = obtenerCliente(solicitud.clienteId());
        Vehiculo vehiculo = obtenerVehiculo(solicitud.vehiculoId());

        validarPropietario(cliente, vehiculo);

        TablaTarifaria tablaTarifaria = obtenerTablaTarifaria(vehiculo);
        ResultadoTarificacion resultado =
                calcularTarificacion(solicitud, cliente, vehiculo, tablaTarifaria);

        Cotizacion cotizacion = crearCotizacion(solicitud, resultado, tablaTarifaria);
        Cotizacion cotizacionGuardada = cotizacionRepository.guardar(cotizacion);

        return toResponse(cotizacionGuardada, resultado);
    }

    private Cliente obtenerCliente(UUID clienteId) {
        return clienteRepository
                .buscarPorId(clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente"));
    }

    private Vehiculo obtenerVehiculo(UUID vehiculoId) {
        return vehiculoRepository
                .buscarPorId(vehiculoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Vehículo"));
    }

    private void validarPropietario(Cliente cliente, Vehiculo vehiculo) {
        if (!vehiculo.getClienteId().equals(cliente.getId())) {
            throw new ReglaNegocioException(
                    "VEHICULO_NO_PERTENECE", "El vehículo no pertenece al cliente");
        }
    }

    private TablaTarifaria obtenerTablaTarifaria(Vehiculo vehiculo) {
        return tablaTarifariaRepository
                .buscarVigente(vehiculo.getTipo(), vehiculo.getUso(), LocalDate.now())
                .orElseThrow(
                        () ->
                                new ReglaNegocioException(
                                        "TABLA_NO_DISPONIBLE",
                                        "No existe tabla tarifaria vigente"));
    }

    private ResultadoTarificacion calcularTarificacion(
            CrearCotizacionRequestModel solicitud,
            Cliente cliente,
            Vehiculo vehiculo,
            TablaTarifaria tablaTarifaria) {
        return motorDeTarificacion.calcular(
                tablaTarifaria,
                cliente,
                vehiculo,
                solicitud.siniestrosResponsables(),
                valorOInicial(solicitud.porcentajeGastos(), "0.10"),
                valorOInicial(solicitud.porcentajeRecargo(), "0.03"),
                valorOInicial(solicitud.porcentajeDescuento(), "0.00"));
    }

    private Cotizacion crearCotizacion(
            CrearCotizacionRequestModel solicitud,
            ResultadoTarificacion resultado,
            TablaTarifaria tablaTarifaria) {
        LocalDateTime fechaCreacion = LocalDateTime.now();
        String numeroCotizacion =
                "COT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return new Cotizacion(
                UUID.randomUUID(),
                numeroCotizacion,
                solicitud.clienteId(),
                solicitud.vehiculoId(),
                tablaTarifaria.getId(),
                resultado.primaComercial(),
                fechaCreacion,
                fechaCreacion.plusDays(15),
                EstadoCotizacion.VIGENTE,
                resultado);
    }

    private BigDecimal valorOInicial(BigDecimal valor, String valorInicial) {
        return valor == null ? new BigDecimal(valorInicial) : valor;
    }
}
