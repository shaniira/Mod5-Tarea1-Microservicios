package com.andinaseguros.notification.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;

/**
 * Pruebas de contrato de eventos (fase 7, paso 7.6): notification solo consume, así que compara lo
 * que lee con los ejemplos de contracts/events. Mismo criterio que en los demás servicios: en CI
 * contracts/ es obligatorio (CONTRATOS_OBLIGATORIOS=true); en el build de la imagen se salta.
 */
public final class Contratos {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SchemaValidatorsConfig CONFIG =
            SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();

    private Contratos() {}

    public static Path carpeta() {
        String propiedad = System.getProperty("contratos.dir");
        if (propiedad != null) {
            return Path.of(propiedad);
        }
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isDirectory(p.resolve("contracts").resolve("events"))) {
                return p.resolve("contracts");
            }
        }
        if ("true".equalsIgnoreCase(System.getenv("CONTRATOS_OBLIGATORIOS"))) {
            throw new IllegalStateException("No se encontró contracts/ y CONTRATOS_OBLIGATORIOS=true");
        }
        Assumptions.abort("Sin contracts/ (build de la imagen): las pruebas de contrato corren en CI");
        return null;
    }

    /** Ejemplo de un evento (contracts/events/ejemplos/), validado contra su esquema. */
    public static JsonNode ejemplo(String routingKey) {
        JsonNode ejemplo = leer(carpeta().resolve("events").resolve("ejemplos").resolve(routingKey + ".json"));
        Set<ValidationMessage> errores = esquema(carpeta(), routingKey).validate(ejemplo);
        assertThat(errores).as("El ejemplo de %s no cumple su esquema", routingKey).isEmpty();
        return ejemplo;
    }

    /**
     * Los $id de los esquemas son https://andinaseguros.example/contracts/...: se mapean a la
     * carpeta local para que los $ref internos (por ejemplo #/$defs/customer) no se descarguen.
     */
    private static JsonSchema esquema(Path carpeta, String routingKey) {
        String local = carpeta.toUri().toString();
        String base = local.endsWith("/") ? local : local + "/";
        JsonSchemaFactory fabrica =
                JsonSchemaFactory.getInstance(
                        SpecVersion.VersionFlag.V202012,
                        b -> b.schemaMappers(m -> m.mapPrefix("https://andinaseguros.example/contracts/", base)));
        return fabrica.getSchema(
                SchemaLocation.of("https://andinaseguros.example/contracts/events/" + routingKey + ".schema.json"), CONFIG);
    }

    private static JsonNode leer(Path archivo) {
        try {
            return JSON.readTree(archivo.toFile());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer " + archivo, e);
        }
    }
}
