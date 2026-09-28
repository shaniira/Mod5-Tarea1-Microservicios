package com.andinaseguros.identity.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pruebas de contrato (fase 7, paso 7.6): el código se compara con contracts/ (JSON Schema de los
 * eventos y OpenAPI de las APIs). Un cambio incompatible rompe el build.
 *
 * <p>contracts/ está en la raíz del repositorio. En CI (checkout completo) se encuentra y las
 * pruebas son obligatorias (CONTRATOS_OBLIGATORIOS=true). Al construir la imagen Docker el contexto
 * es solo el servicio: no hay contracts/ y estas pruebas se saltan.
 */
public final class Contratos {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SchemaValidatorsConfig CONFIG =
            SchemaValidatorsConfig.builder().formatAssertionsEnabled(true).build();

    private Contratos() {}

    /** Carpeta contracts/ del repositorio; si no está, la prueba se salta (o falla en CI). */
    public static Path carpeta() {
        Path carpeta = buscarCarpeta();
        if (carpeta == null) {
            Assumptions.abort("Sin contracts/ (build de la imagen): las pruebas de contrato corren en CI");
        }
        return carpeta;
    }

    /** null si no hay contracts/ (build de la imagen); en CI falla si falta. */
    private static Path buscarCarpeta() {
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
        return null;
    }

    /** El evento (tal como sale a RabbitMQ) cumple contracts/events/<routingKey>.schema.json. */
    public static void validarEvento(String routingKey, JsonNode evento) {
        Path carpeta = buscarCarpeta();
        if (carpeta == null) {
            return; // build de la imagen: las demás aserciones de la prueba siguen corriendo
        }
        Path archivo = carpeta.resolve("events").resolve(routingKey + ".schema.json");
        JsonSchema esquema = esquema(carpeta, routingKey);
        Set<ValidationMessage> errores = esquema.validate(evento);
        assertThat(errores).as("%s no cumple %s: %s", routingKey, archivo.getFileName(), evento).isEmpty();
    }

    /** Ejemplo mínimo de un evento (contracts/events/ejemplos/), validado contra su esquema. */
    public static JsonNode ejemplo(String routingKey) {
        JsonNode ejemplo = leer(carpeta().resolve("events").resolve("ejemplos").resolve(routingKey + ".json"));
        validarEvento(routingKey, ejemplo);
        return ejemplo;
    }

    /**
     * Diferencias incompatibles entre los controladores del paquete y contracts/openapi/<archivo>:
     * endpoints quitados o nuevos sin contrato, campos de respuesta quitados y campos obligatorios
     * nuevos en el cuerpo. Lista vacía = el código cumple el contrato.
     */
    public static List<String> diferenciasApi(String archivoOpenApi, String paqueteControladores) {
        JsonNode api = leer(carpeta().resolve("openapi").resolve(archivoOpenApi));
        Map<String, Method> codigo = endpoints(paqueteControladores);
        Map<String, JsonNode> contrato = new LinkedHashMap<>();
        api.path("paths").fields().forEachRemaining(ruta -> ruta.getValue().fields().forEachRemaining(
                op -> contrato.put(op.getKey().toUpperCase() + " " + normalizar(ruta.getKey()), op.getValue())));

        List<String> diferencias = new ArrayList<>();
        for (String endpoint : contrato.keySet()) {
            if (!codigo.containsKey(endpoint)) {
                diferencias.add("El código ya no tiene " + endpoint + " (cambio incompatible)");
            }
        }
        for (String endpoint : codigo.keySet()) {
            if (!contrato.containsKey(endpoint)) {
                diferencias.add("Endpoint nuevo sin contrato: " + endpoint + " (agregarlo en contracts/openapi)");
            }
        }
        contrato.forEach((endpoint, operacion) -> {
            Method metodo = codigo.get(endpoint);
            if (metodo != null) {
                diferencias.addAll(compararCuerpo(api, endpoint, operacion, metodo));
                diferencias.addAll(compararRespuesta(api, endpoint, operacion, metodo));
            }
        });
        return diferencias;
    }

    private static List<String> compararCuerpo(JsonNode api, String endpoint, JsonNode operacion, Method metodo) {
        List<String> diferencias = new ArrayList<>();
        Class<?> cuerpo = null;
        for (Parameter parametro : metodo.getParameters()) {
            if (parametro.isAnnotationPresent(RequestBody.class)) {
                cuerpo = parametro.getType();
            }
        }
        JsonNode esquema = resolver(api, operacion.path("requestBody").path("content").path("application/json").path("schema"));
        if (cuerpo == null || esquema == null) {
            return diferencias;
        }
        Set<String> propiedades = propiedades(cuerpo);
        Set<String> requeridos = new TreeSet<>();
        esquema.path("required").forEach(r -> requeridos.add(r.asText()));
        esquema.path("properties").fieldNames().forEachRemaining(campo -> {
            if (!propiedades.contains(campo)) {
                diferencias.add(endpoint + ": el cuerpo ya no acepta '" + campo + "' (cambio incompatible)");
            }
        });
        for (String obligatorio : obligatorios(cuerpo)) {
            if (!requeridos.contains(obligatorio)) {
                diferencias.add(endpoint + ": campo obligatorio nuevo '" + obligatorio + "' (cambio incompatible)");
            }
        }
        return diferencias;
    }

    private static List<String> compararRespuesta(JsonNode api, String endpoint, JsonNode operacion, Method metodo) {
        List<String> diferencias = new ArrayList<>();
        JsonNode respuestas = operacion.path("responses");
        Iterator<String> codigos = respuestas.fieldNames();
        while (codigos.hasNext()) {
            String codigo = codigos.next();
            if (!codigo.startsWith("2")) {
                continue;
            }
            JsonNode contenido = respuestas.path(codigo).path("content");
            JsonNode esquema = contenido.elements().hasNext() ? contenido.elements().next().path("schema") : null;
            if (esquema != null && "array".equals(esquema.path("type").asText())) {
                esquema = esquema.path("items");
            }
            esquema = resolver(api, esquema);
            Class<?> tipo = tipoRespuesta(metodo.getGenericReturnType());
            if (esquema == null || tipo == null) {
                continue;
            }
            Set<String> propiedades = propiedades(tipo);
            esquema.path("properties").fieldNames().forEachRemaining(campo -> {
                if (!propiedades.contains(campo)) {
                    diferencias.add(endpoint + ": la respuesta ya no trae '" + campo + "' (cambio incompatible)");
                }
            });
        }
        return diferencias;
    }

    private static Map<String, Method> endpoints(String paquete) {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Map<String, Method> endpoints = new LinkedHashMap<>();
        for (BeanDefinition definicion : escaner.findCandidateComponents(paquete)) {
            Class<?> controlador;
            try {
                controlador = Class.forName(definicion.getBeanClassName());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(controlador, RequestMapping.class);
            String[] prefijos = base == null || base.path().length == 0 ? new String[] {""} : base.path();
            for (Method metodo : controlador.getDeclaredMethods()) {
                RequestMapping mapeo = AnnotatedElementUtils.findMergedAnnotation(metodo, RequestMapping.class);
                if (mapeo == null) {
                    continue;
                }
                String[] rutas = mapeo.path().length == 0 ? new String[] {""} : mapeo.path();
                for (String prefijo : prefijos) {
                    for (String ruta : rutas) {
                        for (var verbo : mapeo.method()) {
                            endpoints.put(verbo.name() + " " + normalizar(prefijo + ruta), metodo);
                        }
                    }
                }
            }
        }
        return endpoints;
    }

    /** /api/polizas/{polizaId}/siniestros y /api/polizas/{id}/siniestros son la misma ruta. */
    private static String normalizar(String ruta) {
        String sinVariables = ruta.replaceAll("\\{[^}]+}", "{}");
        return sinVariables.length() > 1 && sinVariables.endsWith("/")
                ? sinVariables.substring(0, sinVariables.length() - 1)
                : sinVariables;
    }

    private static Class<?> tipoRespuesta(Type tipo) {
        if (tipo instanceof ParameterizedType parametrizado) {
            Class<?> crudo = (Class<?>) parametrizado.getRawType();
            if (ResponseEntity.class.equals(crudo) || Collection.class.isAssignableFrom(crudo)) {
                return tipoRespuesta(parametrizado.getActualTypeArguments()[0]);
            }
            return null;
        }
        if (tipo instanceof Class<?> clase && !clase.isPrimitive() && !clase.getName().startsWith("java.")) {
            return clase;
        }
        return null;
    }

    private static Set<String> propiedades(Class<?> tipo) {
        Set<String> nombres = new LinkedHashSet<>();
        if (tipo.isRecord()) {
            for (RecordComponent componente : tipo.getRecordComponents()) {
                JsonProperty renombre = componente.getAccessor().getAnnotation(JsonProperty.class);
                nombres.add(renombre != null && !renombre.value().isEmpty() ? renombre.value() : componente.getName());
            }
            return nombres;
        }
        for (Class<?> c = tipo; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field campo : c.getDeclaredFields()) {
                if (!Modifier.isStatic(campo.getModifiers())) {
                    JsonProperty renombre = campo.getAnnotation(JsonProperty.class);
                    nombres.add(renombre != null && !renombre.value().isEmpty() ? renombre.value() : campo.getName());
                }
            }
        }
        return nombres;
    }

    private static Set<String> obligatorios(Class<?> tipo) {
        Set<String> nombres = new TreeSet<>();
        for (Class<?> c = tipo; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field campo : c.getDeclaredFields()) {
                if (campo.isAnnotationPresent(NotNull.class) || campo.isAnnotationPresent(NotBlank.class)
                        || campo.isAnnotationPresent(NotEmpty.class)) {
                    nombres.add(campo.getName());
                }
            }
        }
        return nombres;
    }

    private static JsonNode resolver(JsonNode api, JsonNode esquema) {
        if (esquema == null || esquema.isMissingNode()) {
            return null;
        }
        String ref = esquema.path("$ref").asText("");
        if (ref.startsWith("#/components/schemas/")) {
            return api.path("components").path("schemas").path(ref.substring("#/components/schemas/".length()));
        }
        return esquema.has("properties") ? esquema : null;
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
