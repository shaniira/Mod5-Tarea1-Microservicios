package com.andinaseguros.frameworksdrivers.configuration.spring;

import com.andinaseguros.entities.enums.*;
import com.andinaseguros.entities.model.Usuario;
import com.andinaseguros.usecases.port.out.repository.UsuarioRepository;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.document.*;
import com.andinaseguros.interfaceadapters.out.persistence.mongodb.repository.*;
import com.andinaseguros.usecases.port.out.security.PasswordEncoderPort;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Datos reproducibles para demostraciones locales. No elimina información creada por el usuario. */
@Component
public class MongoDemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MongoDemoDataInitializer.class);
    private static final String CLIENTE_ANA = "10000000-0000-0000-0000-000000000001";
    private static final String CLIENTE_LUIS = "10000000-0000-0000-0000-000000000002";
    private static final String VEHICULO_ANA = "20000000-0000-0000-0000-000000000001";
    private static final String VEHICULO_LUIS = "20000000-0000-0000-0000-000000000002";
    private static final String TARIFA_AUTO = "30000000-0000-0000-0000-000000000001";
    private static final String TARIFA_CAMIONETA = "30000000-0000-0000-0000-000000000002";
    private static final String TARIFA_BORRADOR = "30000000-0000-0000-0000-000000000003";
    private static final String COT_VIGENTE = "40000000-0000-0000-0000-000000000001";
    private static final String COT_ACEPTADA = "40000000-0000-0000-0000-000000000002";
    private static final String COT_EMITIDA = "40000000-0000-0000-0000-000000000003";
    private static final String COT_VENCIDA = "40000000-0000-0000-0000-000000000004";
    private static final String COT_POL_PENDIENTE = "40000000-0000-0000-0000-000000000005";
    private static final String COT_POL_VENCIDA = "40000000-0000-0000-0000-000000000006";
    private static final String COT_POL_RENOVADA = "40000000-0000-0000-0000-000000000007";
    private static final String POL_PENDIENTE = "50000000-0000-0000-0000-000000000001";
    private static final String POL_VIGENTE = "50000000-0000-0000-0000-000000000002";
    private static final String POL_VENCIDA = "50000000-0000-0000-0000-000000000003";
    private static final String POL_RENOVADA = "50000000-0000-0000-0000-000000000004";

    private final UsuarioRepository usuarios;
    private final PasswordEncoderPort passwordEncoder;
    private final SpringDataClienteMongoRepository clientes;
    private final SpringDataVehiculoMongoRepository vehiculos;
    private final SpringDataTablaTarifariaMongoRepository tarifas;
    private final SpringDataCotizacionMongoRepository cotizaciones;
    private final SpringDataPolizaMongoRepository polizas;
    private final SpringDataRenovacionMongoRepository renovaciones;

    public MongoDemoDataInitializer(
            UsuarioRepository usuarios,
            PasswordEncoderPort passwordEncoder,
            SpringDataClienteMongoRepository clientes,
            SpringDataVehiculoMongoRepository vehiculos,
            SpringDataTablaTarifariaMongoRepository tarifas,
            SpringDataCotizacionMongoRepository cotizaciones,
            SpringDataPolizaMongoRepository polizas,
            SpringDataRenovacionMongoRepository renovaciones) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.clientes = clientes;
        this.vehiculos = vehiculos;
        this.tarifas = tarifas;
        this.cotizaciones = cotizaciones;
        this.polizas = polizas;
        this.renovaciones = renovaciones;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin();
        clientes.saveAll(
                List.of(
                        cliente(
                                CLIENTE_ANA,
                                "DNI",
                                "70000001",
                                "Ana",
                                "Torres",
                                "ana.demo@andina.local"),
                        cliente(
                                CLIENTE_LUIS,
                                "DNI",
                                "70000002",
                                "Luis",
                                "Mendoza",
                                "luis.demo@andina.local")));
        List<ClienteDocument> todosLosClientes = clientes.findAll();
        todosLosClientes.forEach(cliente -> cliente.telefono = "921175206");
        clientes.saveAll(todosLosClientes);
        vehiculos.saveAll(
                List.of(
                        vehiculo(
                                VEHICULO_ANA,
                                CLIENTE_ANA,
                                "DEM-001",
                                "Toyota",
                                "Corolla",
                                2023,
                                TipoVehiculo.AUTO,
                                TipoUso.PARTICULAR),
                        vehiculo(
                                VEHICULO_LUIS,
                                CLIENTE_LUIS,
                                "DEM-002",
                                "Hyundai",
                                "Tucson",
                                2022,
                                TipoVehiculo.CAMIONETA,
                                TipoUso.PARTICULAR)));
        tarifas.saveAll(
                List.of(
                        tarifa(
                                TARIFA_AUTO,
                                "DEMO-AUTO",
                                1,
                                TipoVehiculo.AUTO,
                                TipoUso.PARTICULAR,
                                "1250.00",
                                EstadoTablaTarifaria.VIGENTE),
                        tarifa(
                                TARIFA_CAMIONETA,
                                "DEMO-CAMIONETA",
                                1,
                                TipoVehiculo.CAMIONETA,
                                TipoUso.PARTICULAR,
                                "1680.00",
                                EstadoTablaTarifaria.VIGENTE),
                        tarifa(
                                TARIFA_BORRADOR,
                                "DEMO-AUTO",
                                2,
                                TipoVehiculo.AUTO,
                                TipoUso.PARTICULAR,
                                "1320.00",
                                EstadoTablaTarifaria.BORRADOR)));

        LocalDateTime now = LocalDateTime.now().withNano(0);
        cotizaciones.saveAll(
                List.of(
                        cotizacion(
                                COT_VIGENTE,
                                "COT-DEMO-001",
                                CLIENTE_ANA,
                                VEHICULO_ANA,
                                TARIFA_AUTO,
                                "1375.00",
                                now.minusDays(1),
                                now.plusDays(14),
                                EstadoCotizacion.VIGENTE),
                        cotizacion(
                                COT_ACEPTADA,
                                "COT-DEMO-002",
                                CLIENTE_LUIS,
                                VEHICULO_LUIS,
                                TARIFA_CAMIONETA,
                                "1848.00",
                                now.minusDays(2),
                                now.plusDays(13),
                                EstadoCotizacion.ACEPTADA),
                        cotizacion(
                                COT_EMITIDA,
                                "COT-DEMO-003",
                                CLIENTE_ANA,
                                VEHICULO_ANA,
                                TARIFA_AUTO,
                                "1425.00",
                                now.minusDays(20),
                                now.minusDays(5),
                                EstadoCotizacion.EMITIDA),
                        cotizacion(
                                COT_VENCIDA,
                                "COT-DEMO-004",
                                CLIENTE_LUIS,
                                VEHICULO_LUIS,
                                TARIFA_CAMIONETA,
                                "1900.00",
                                now.minusDays(40),
                                now.minusDays(25),
                                EstadoCotizacion.VENCIDA),
                        cotizacion(
                                COT_POL_PENDIENTE,
                                "COT-DEMO-005",
                                CLIENTE_LUIS,
                                VEHICULO_LUIS,
                                TARIFA_CAMIONETA,
                                "1848.00",
                                now.minusDays(5),
                                now.plusDays(10),
                                EstadoCotizacion.EMITIDA),
                        cotizacion(
                                COT_POL_VENCIDA,
                                "COT-DEMO-006",
                                CLIENTE_LUIS,
                                VEHICULO_LUIS,
                                TARIFA_CAMIONETA,
                                "1750.00",
                                now.minusYears(1),
                                now.minusMonths(11),
                                EstadoCotizacion.EMITIDA),
                        cotizacion(
                                COT_POL_RENOVADA,
                                "COT-DEMO-007",
                                CLIENTE_ANA,
                                VEHICULO_ANA,
                                TARIFA_AUTO,
                                "1350.00",
                                now.minusYears(1),
                                now.minusMonths(11),
                                EstadoCotizacion.EMITIDA)));

        LocalDate today = LocalDate.now();
        // Se recrean solo las pólizas controladas por este inicializador. Esto evita
        // conflictos transitorios del índice único al cambiar cotizaciones entre reinicios.
        polizas.deleteAllById(List.of(POL_PENDIENTE, POL_VIGENTE, POL_VENCIDA, POL_RENOVADA));
        polizas.saveAll(
                List.of(
                        poliza(
                                POL_PENDIENTE,
                                "POL-DEMO-001",
                                COT_POL_PENDIENTE,
                                CLIENTE_LUIS,
                                VEHICULO_LUIS,
                                "1848.00",
                                today,
                                today.plusYears(1),
                                EstadoPoliza.PENDIENTE_PAGO,
                                null),
                        poliza(
                                POL_VIGENTE,
                                "POL-DEMO-002",
                                COT_EMITIDA,
                                CLIENTE_ANA,
                                VEHICULO_ANA,
                                "1425.00",
                                today.minusMonths(2),
                                today.plusMonths(10),
                                EstadoPoliza.VIGENTE,
                                null),
                        poliza(
                                POL_VENCIDA,
                                "POL-DEMO-003",
                                COT_POL_VENCIDA,
                                CLIENTE_LUIS,
                                VEHICULO_LUIS,
                                "1750.00",
                                today.minusYears(1).minusDays(10),
                                today.minusDays(10),
                                EstadoPoliza.VENCIDA,
                                null),
                        poliza(
                                POL_RENOVADA,
                                "POL-DEMO-004",
                                COT_POL_RENOVADA,
                                CLIENTE_ANA,
                                VEHICULO_ANA,
                                "1350.00",
                                today.minusYears(1),
                                today,
                                EstadoPoliza.RENOVADA,
                                null)));

        renovaciones.saveAll(
                List.of(
                        renovacion(
                                "60000000-0000-0000-0000-000000000001",
                                POL_VENCIDA,
                                "1750.00",
                                "1837.50",
                                EstadoRenovacion.PENDIENTE,
                                "Pendiente de decisión del cliente",
                                now.minusDays(2),
                                now.plusDays(13),
                                null),
                        renovacion(
                                "60000000-0000-0000-0000-000000000002",
                                POL_VIGENTE,
                                "1425.00",
                                "1496.25",
                                EstadoRenovacion.AUTOMATICA,
                                "Buen historial sin siniestros",
                                now.minusDays(1),
                                now.plusDays(14),
                                null),
                        renovacion(
                                "60000000-0000-0000-0000-000000000003",
                                POL_RENOVADA,
                                "1350.00",
                                "1417.50",
                                EstadoRenovacion.ACEPTADA,
                                "Renovación aceptada",
                                now.minusDays(20),
                                now.minusDays(5),
                                POL_VIGENTE),
                        renovacion(
                                "60000000-0000-0000-0000-000000000004",
                                POL_VENCIDA,
                                "1750.00",
                                "2100.00",
                                EstadoRenovacion.RECHAZADA,
                                "Cliente rechazó la propuesta",
                                now.minusDays(40),
                                now.minusDays(25),
                                null)));
        log.info(
                "Datos demo Mongo listos: {} tarifas, {} cotizaciones, {} pólizas y {} renovaciones"
                        + " DEMO",
                3,
                7,
                4,
                4);
    }

    private void seedAdmin() {
        if (usuarios.buscarPorUsername("admin").isEmpty()) {
            usuarios.guardar(
                    new Usuario(
                            UUID.fromString("00000000-0000-0000-0000-000000000001"),
                            "admin",
                            null,
                            passwordEncoder.codificar("Admin123*"),
                            null,
                            RolUsuario.ADMIN,
                            true,
                            null,
                            false));
        }
    }

    private static ClienteDocument cliente(
            String id,
            String tipo,
            String documento,
            String nombres,
            String apellidos,
            String correo) {
        ClienteDocument d = new ClienteDocument();
        d.id = id;
        d.tipoDocumento = tipo;
        d.numeroDocumento = documento;
        d.nombres = nombres;
        d.apellidos = apellidos;
        d.fechaNacimiento = LocalDate.of(1990, 1, 15);
        d.correo = correo;
        d.telefono = "921175206";
        d.activo = true;
        return d;
    }

    private static VehiculoDocument vehiculo(
            String id,
            String clienteId,
            String placa,
            String marca,
            String modelo,
            int anio,
            TipoVehiculo tipo,
            TipoUso uso) {
        VehiculoDocument d = new VehiculoDocument();
        d.id = id;
        d.clienteId = clienteId;
        d.placa = placa;
        d.marca = marca;
        d.modelo = modelo;
        d.anioFabricacion = anio;
        d.tipo = tipo;
        d.uso = uso;
        d.zonaCirculacion = "LIMA";
        return d;
    }

    private static TablaTarifariaDocument tarifa(
            String id,
            String codigo,
            int version,
            TipoVehiculo tipo,
            TipoUso uso,
            String prima,
            EstadoTablaTarifaria estado) {
        TablaTarifariaDocument d = new TablaTarifariaDocument();
        d.id = id;
        d.codigo = codigo;
        d.version = version;
        d.tipoVehiculo = tipo;
        d.tipoUso = uso;
        d.primaBase = new BigDecimal(prima);
        d.primaMinima = new BigDecimal("900.00");
        d.inicioVigencia = LocalDate.of(2025, 1, 1);
        d.finVigencia = LocalDate.of(2030, 12, 31);
        d.codigoNotaTecnica = "NT-DEMO-2026";
        d.estado = estado;
        FactorRiesgoDocument factor = new FactorRiesgoDocument();
        factor.id =
                UUID.nameUUIDFromBytes((id + "-F1").getBytes(StandardCharsets.UTF_8)).toString();
        factor.codigo = "ANTIGUEDAD";
        factor.nombre = "Antigüedad del vehículo";
        factor.tipoVariable = "ANIOS";
        factor.valorMinimo = BigDecimal.ZERO;
        factor.valorMaximo = new BigDecimal("5");
        factor.multiplicador = new BigDecimal("1.10");
        factor.orden = 1;
        d.factores = List.of(factor);
        return d;
    }

    private static CotizacionDocument cotizacion(
            String id,
            String numero,
            String clienteId,
            String vehiculoId,
            String tarifaId,
            String prima,
            LocalDateTime creada,
            LocalDateTime expira,
            EstadoCotizacion estado) {
        CotizacionDocument d = new CotizacionDocument();
        d.id = id;
        d.numero = numero;
        d.clienteId = clienteId;
        d.vehiculoId = vehiculoId;
        d.tablaTarifariaId = tarifaId;
        d.prima = new BigDecimal(prima);
        d.moneda = "PEN";
        d.fechaCreacion = creada;
        d.fechaExpiracion = expira;
        d.estado = estado;
        d.desgloseJson = null;
        return d;
    }

    private static PolizaDocument poliza(
            String id,
            String numero,
            String cotizacionId,
            String clienteId,
            String vehiculoId,
            String prima,
            LocalDate inicio,
            LocalDate fin,
            EstadoPoliza estado,
            String renovacionOrigenId) {
        PolizaDocument d = new PolizaDocument();
        d.id = id;
        d.numero = numero;
        d.cotizacionId = cotizacionId;
        d.clienteId = clienteId;
        d.vehiculoId = vehiculoId;
        d.prima = new BigDecimal(prima);
        d.moneda = "PEN";
        d.inicioVigencia = inicio;
        d.finVigencia = fin;
        d.estado = estado;
        d.renovacionOrigenId = renovacionOrigenId;
        return d;
    }

    private static RenovacionDocument renovacion(
            String id,
            String polizaId,
            String anterior,
            String nueva,
            EstadoRenovacion estado,
            String motivo,
            LocalDateTime creada,
            LocalDateTime vence,
            String polizaRenovadaId) {
        RenovacionDocument d = new RenovacionDocument();
        d.id = id;
        d.polizaOrigenId = polizaId;
        d.primaAnterior = new BigDecimal(anterior);
        d.nuevaPrima = new BigDecimal(nueva);
        d.porcentajeVariacion =
                d.nuevaPrima
                        .subtract(d.primaAnterior)
                        .multiply(new BigDecimal("100"))
                        .divide(d.primaAnterior, 2, java.math.RoundingMode.HALF_UP);
        d.siniestrosConsiderados = 0;
        d.estado = estado;
        d.motivo = motivo;
        d.creadaEn = creada;
        d.venceEn = vence;
        d.decididaEn =
                estado == EstadoRenovacion.ACEPTADA || estado == EstadoRenovacion.RECHAZADA
                        ? creada.plusDays(2)
                        : null;
        d.polizaRenovadaId = polizaRenovadaId;
        return d;
    }
}
