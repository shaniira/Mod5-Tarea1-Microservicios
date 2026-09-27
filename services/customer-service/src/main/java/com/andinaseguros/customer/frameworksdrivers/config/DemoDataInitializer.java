package com.andinaseguros.customer.frameworksdrivers.config;

import com.andinaseguros.customer.entities.enums.TipoUso;
import com.andinaseguros.customer.entities.enums.TipoVehiculo;
import com.andinaseguros.customer.entities.model.Cliente;
import com.andinaseguros.customer.entities.model.Vehiculo;
import com.andinaseguros.customer.entities.valueobject.Placa;
import com.andinaseguros.customer.usecases.mapper.ClienteEventMapper;
import com.andinaseguros.customer.usecases.mapper.VehiculoEventMapper;
import com.andinaseguros.customer.usecases.port.out.event.DomainEventPublisherPort;
import com.andinaseguros.customer.usecases.port.out.id.IdGeneratorPort;
import com.andinaseguros.customer.usecases.port.out.repository.ClienteRepository;
import com.andinaseguros.customer.usecases.port.out.repository.VehiculoRepository;
import com.andinaseguros.customer.usecases.port.out.time.ClockPort;
import com.andinaseguros.customer.usecases.port.out.transaccion.TransaccionPort;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Clientes y vehículos demo para desarrollo local (APP_DEMO_DATA_ENABLED=true en Compose). Son los
 * mismos que creaba el monolito, con los mismos identificadores: las cotizaciones y pólizas demo del
 * monolito los referencian. Solo se crean si no existen y se publican sus eventos, así llegan a las
 * proyecciones del backend, identity y notification.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final UUID CLIENTE_ANA = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CLIENTE_LUIS = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID VEHICULO_ANA = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID VEHICULO_LUIS = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final String TELEFONO_DEMO = "921175206";

    private final ClienteRepository clientes;
    private final VehiculoRepository vehiculos;
    private final DomainEventPublisherPort eventos;
    private final TransaccionPort transaccion;
    private final ClockPort clock;
    private final IdGeneratorPort ids;

    public DemoDataInitializer(
            ClienteRepository clientes,
            VehiculoRepository vehiculos,
            DomainEventPublisherPort eventos,
            TransaccionPort transaccion,
            ClockPort clock,
            IdGeneratorPort ids) {
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.eventos = eventos;
        this.transaccion = transaccion;
        this.clock = clock;
        this.ids = ids;
    }

    @Override
    public void run(ApplicationArguments args) {
        int creados = 0;
        creados += cliente(CLIENTE_ANA, "70000001", "Ana", "Torres", "ana.demo@andina.local");
        creados += cliente(CLIENTE_LUIS, "70000002", "Luis", "Mendoza", "luis.demo@andina.local");
        creados +=
                vehiculo(VEHICULO_ANA, CLIENTE_ANA, "DEM-001", "Toyota", "Corolla", 2023, TipoVehiculo.AUTO);
        creados +=
                vehiculo(VEHICULO_LUIS, CLIENTE_LUIS, "DEM-002", "Hyundai", "Tucson", 2022, TipoVehiculo.CAMIONETA);
        log.info("Datos demo de clientes listos ({} registros nuevos)", creados);
    }

    private int cliente(UUID id, String documento, String nombres, String apellidos, String correo) {
        if (clientes.buscarPorId(id).isPresent() || clientes.buscarPorDocumento(documento).isPresent()) {
            return 0;
        }
        Cliente cliente =
                new Cliente(
                        id, "DNI", documento, nombres, apellidos, LocalDate.of(1990, 1, 15), correo, TELEFONO_DEMO, true);
        transaccion.ejecutar(
                () -> {
                    Cliente guardado = clientes.guardar(cliente);
                    eventos.publicar(ClienteEventMapper.registrado(ids.generar(), clock.now(), guardado));
                    return guardado;
                });
        return 1;
    }

    private int vehiculo(
            UUID id, UUID clienteId, String placa, String marca, String modelo, int anio, TipoVehiculo tipo) {
        if (vehiculos.buscarPorId(id).isPresent() || vehiculos.buscarPorPlaca(placa).isPresent()) {
            return 0;
        }
        Vehiculo vehiculo =
                new Vehiculo(id, clienteId, new Placa(placa), marca, modelo, anio, tipo, TipoUso.PARTICULAR, "LIMA");
        transaccion.ejecutar(
                () -> {
                    Vehiculo guardado = vehiculos.guardar(vehiculo);
                    eventos.publicar(VehiculoEventMapper.registrado(ids.generar(), clock.now(), guardado));
                    return guardado;
                });
        return 1;
    }
}
