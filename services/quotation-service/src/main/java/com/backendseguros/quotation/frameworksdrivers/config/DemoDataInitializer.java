package com.backendseguros.quotation.frameworksdrivers.config;

import com.backendseguros.quotation.entities.enums.EstadoTablaTarifaria;
import com.backendseguros.quotation.entities.enums.TipoUso;
import com.backendseguros.quotation.entities.enums.TipoVehiculo;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.document.FactorRiesgoDocument;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.document.TablaTarifariaDocument;
import com.backendseguros.quotation.interfaceadapters.out.persistence.mongodb.repository.SpringDataTablaTarifariaMongoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Tablas tarifarias demo (APP_DEMO_DATA_ENABLED=true en Compose). Sin tablas no se puede cotizar, y
 * hasta la fase 7 solo llegaban migrando la base del monolito; al retirarlo del repositorio, una
 * instalación nueva las siembra aquí. Son las mismas del monolito, con los mismos ids: si ya
 * existen (migradas o sembradas antes) no se tocan.
 */
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    static final String TARIFA_AUTO = "30000000-0000-0000-0000-000000000001";
    static final String TARIFA_CAMIONETA = "30000000-0000-0000-0000-000000000002";
    static final String TARIFA_BORRADOR = "30000000-0000-0000-0000-000000000003";

    private final SpringDataTablaTarifariaMongoRepository tarifas;

    public DemoDataInitializer(SpringDataTablaTarifariaMongoRepository tarifas) {
        this.tarifas = tarifas;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<TablaTarifariaDocument> demo =
                List.of(
                        tarifa(TARIFA_AUTO, "DEMO-AUTO", 1, TipoVehiculo.AUTO, "1250.00", EstadoTablaTarifaria.VIGENTE),
                        tarifa(TARIFA_CAMIONETA, "DEMO-CAMIONETA", 1, TipoVehiculo.CAMIONETA, "1680.00",
                                EstadoTablaTarifaria.VIGENTE),
                        tarifa(TARIFA_BORRADOR, "DEMO-AUTO", 2, TipoVehiculo.AUTO, "1320.00", EstadoTablaTarifaria.BORRADOR));
        int creadas = 0;
        for (TablaTarifariaDocument tabla : demo) {
            if (!tarifas.existsById(tabla.id)) {
                tarifas.save(tabla);
                creadas++;
            }
        }
        log.info("Tablas tarifarias demo listas ({} nuevas)", creadas);
    }

    static TablaTarifariaDocument tarifa(
            String id, String codigo, int version, TipoVehiculo tipo, String prima, EstadoTablaTarifaria estado) {
        TablaTarifariaDocument d = new TablaTarifariaDocument();
        d.id = id;
        d.codigo = codigo;
        d.version = version;
        d.tipoVehiculo = tipo;
        d.tipoUso = TipoUso.PARTICULAR;
        d.primaBase = new BigDecimal(prima);
        d.primaMinima = new BigDecimal("900.00");
        d.inicioVigencia = LocalDate.of(2025, 1, 1);
        d.finVigencia = LocalDate.of(2030, 12, 31);
        d.codigoNotaTecnica = "NT-DEMO-2026";
        d.estado = estado;
        FactorRiesgoDocument factor = new FactorRiesgoDocument();
        factor.id = UUID.nameUUIDFromBytes((id + "-F1").getBytes(StandardCharsets.UTF_8)).toString();
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
}
