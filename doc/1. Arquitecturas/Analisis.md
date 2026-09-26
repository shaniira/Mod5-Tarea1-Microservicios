

---

## Arquitectura Clean

### Reglas que cumple (`CleanArchitectureTest`, ArchUnit — 8 reglas, todas en verde)

1. **`entities_are_independent`** — `entities` no depende de `usecases`, `interfaceadapters` ni `frameworksdrivers`.
   Sustenta la *Dependency Rule* de Robert C. Martin: las reglas de negocio empresariales (el anillo más interno) no conocen nada de lo que está afuera.
2. **`use_cases_only_point_inward`** — `usecases` no depende de `interfaceadapters` ni `frameworksdrivers`.
   Garantiza que los casos de uso solo conocen `entities`; el mecanismo de entrega (REST, CLI, mensajería) puede cambiar sin tocarlos.
3. **`interface_adapters_do_not_know_frameworks`** — `interfaceadapters` no depende de `frameworksdrivers`.
   Los controllers y gateways no dependen de la configuración/arranque de Spring; se respeta el orden de flechas de dependencia entre los cuatro anillos.
4. **`entities_have_no_framework_dependencies`** — `entities` no importa `org.springframework`, `org.bson` ni `com.mongodb`.
   Las entidades son POJOs puros, sin acoplamiento a infraestructura de persistencia o framework.
5. **`use_cases_have_no_framework_dependencies`** — `usecases` no importa `org.springframework`, `jakarta.persistence`, `jakarta.validation`, `org.bson` ni `com.mongodb`.
   Los casos de uso son agnósticos incluso a la validación HTTP: el `RequestModel` interno es un POJO sin anotaciones, distinto del `Request` validado que vive en `interfaceadapters.in.rest.request`.
6. **`output_ports_are_interfaces`** — toda clase `*Port`/`*Repository` en `usecases.port.out` debe ser interfaz.
   Verifica inversión de dependencia real: los casos de uso dependen de abstracciones (puertos de salida), nunca de implementaciones concretas (Mongo, JWT, etc.).
7. **`controllers_live_in_interface_adapters`** — toda clase `*Controller` reside en `interfaceadapters.in.rest`.
   El mecanismo de entrega HTTP queda contenido en el anillo que le corresponde (Interface Adapters), no se filtra a `usecases`.
8. **`entities_have_no_repository_package`** — prohíbe un paquete `entities.repository`.
   Evita que el acceso a datos se cuele en la capa de entidades bajo un nombre engañoso.

### Otras validaciones estructurales que sustentan la arquitectura

- **Composition root explícito**: `frameworksdrivers/configuration/spring/UseCaseConfig.java` construye los casos de uso manualmente (`new XxxUseCase(...)`), sin `@Service`/`@Autowired` dentro de `usecases` — el cableado de dependencias vive en el anillo más externo.
- **Separación Request / RequestModel**: cada operación tiene un `*Request` (HTTP, con `jakarta.validation`) en `interfaceadapters` y un `*RequestModel` (interno, sin anotaciones) en `usecases`, mapeados por `RestRequestMapper` — nombres distintos por capa, sin ambigüedad de tipos.
- **Persistencia separada del dominio**: los `*Document` de Mongo viven en `interfaceadapters.out.persistence.mongodb.document`, mapeados hacia/desde las entidades puras mediante mappers dedicados.

---

## Arquitectura Hexagonal

### Reglas que cumple (`HexagonalArchitectureTest`, ArchUnit — 9 reglas, todas en verde)

1. **`core_is_framework_independent`** — `core` no depende de `adapters`, `bootstrap`, Spring, Mongo, Jakarta, JWT, Jackson ni Springdoc.
   Es el requisito central de Ports & Adapters: el núcleo (dominio + aplicación + puertos) es completamente independiente de cualquier framework o adaptador concreto.
2. **`domain_does_not_depend_on_application_or_ports`** — `core.domain` no depende de `core.application` ni `core.ports`.
   El dominio es el círculo más interno dentro del propio núcleo: ni siquiera conoce los contratos de los puertos.
3. **`domain_has_no_repository_package`** — prohíbe `core.domain.repository`.
   Refuerza que los contratos de persistencia viven en `ports.out`, con el vocabulario propio de Hexagonal, no mezclados en el dominio.
4. **`repository_ports_are_core_interfaces`** — todo `*RepositoryPort` en `ports.out.persistence` es interfaz.
5. **`outbound_ports_are_interfaces`** — todo `*Port` en `ports.out` es interfaz (seguridad, notificación, eventos, reloj, generación de ids, etc.), generalizando la regla anterior a todos los puertos de salida.
6. **`inbound_ports_are_interfaces`** — todo lo declarado en `ports.in` es interfaz.
   Cada operación que el sistema ofrece hacia afuera tiene un contrato explícito (*driving port*).
7. **`outbound_port_implementations_live_in_adapters`** — las implementaciones de cualquier `*Port` de salida solo pueden residir en `adapters`.
   Verifica la inversión de dependencia en ambos sentidos: el contrato vive adentro, la implementación vive afuera.
8. **`inbound_port_implementations_live_in_core_application`** — las implementaciones de los puertos de entrada (casos de uso) solo pueden residir en `core.application`.
   Separa de forma verificable el contrato (`ports.in`) de su implementación (`application`).
9. **`rest_controllers_are_inbound_adapters`** — toda clase `*Controller` reside en `adapters.inbound.rest.controller`.

### Otras validaciones estructurales que sustentan la arquitectura

- **Doble adaptador de salida por puerto**: los repositorios tienen implementación `memory` y `mongo` para el mismo `*RepositoryPort`, seleccionadas por `@Profile`, demostrando en código real que Ports & Adapters permite intercambiar adaptadores sin tocar el núcleo.
- **Composition root en `bootstrap`**: el cableado de beans (`UseCaseConfiguration`, `SecurityConfig`, `MongoConfiguration`) vive fuera del hexágono, tal como exige la regla 1.
- **Separación Request / Command**: cada operación tiene un `*Request` (HTTP, validado) en `adapters.inbound.rest.request` y un `*Command` (interno, puro) en `core.application.dto`, mapeados por `RestRequestMapper` — vocabulario de *Command* propio de la combinación Hexagonal + CQRS.

---

## Arquitectura Onion

### Reglas que cumple (`OnionArchitectureTest`, ArchUnit — 9 reglas, todas en verde)

1. **`domain_is_the_independent_center`** — `domain` no depende de `application`, `infrastructure` ni `presentation`.
   El dominio es el centro absoluto de los anillos concéntricos; no conoce ninguna capa externa, ni siquiera la de aplicación.
2. **`application_only_points_to_domain`** — `application` no depende de `infrastructure` ni `presentation`.
   Los servicios de aplicación solo conocen el dominio, respetando la dirección de las flechas hacia el centro.
3. **`application_has_no_framework_dependencies`** — `application` no importa Spring, JPA, Bean Validation ni Mongo.
   Al igual que en Clean, el DTO interno de cada operación (`*Dto`) es un POJO puro; la validación HTTP vive únicamente en `presentation.request` (`*Request`).
4. **`onion_repository_contracts_are_domain_interfaces`** — todo lo declarado en `domain.repository` es interfaz.
   Los contratos de persistencia se definen en el centro (Dependency Inversion clásica de Palermo).
5. **`onion_gateway_contracts_are_application_interfaces`** — todo `*Port` en `application.gateway` es interfaz.
   Los contratos de servicios externos (JSON.pe, WhatsApp, JWT) se definen en la capa de aplicación, no en infraestructura.
6. **`onion_repository_implementations_live_in_infrastructure`** — las implementaciones de `domain.repository` solo pueden residir en `infrastructure`.
7. **`onion_gateway_implementations_live_in_infrastructure`** — las implementaciones de `application.gateway.*Port` solo pueden residir en `infrastructure`.
8. **`onion_has_no_port_packages`** — prohíbe cualquier paquete llamado `port`.
   Coherente con el vocabulario propio de Onion: no se toma prestado el lenguaje formal `port/adapter` de Hexagonal, aunque el efecto (inversión de dependencia) sea el mismo.
9. **`domain_has_no_framework_dependencies`** — `domain` no importa Spring ni Spring Data.

### Otras validaciones estructurales que sustentan la arquitectura

- **Vocabulario propio consistente**: el DTO interno de cada operación se llama `*Dto` (no `*Command`, término tomado de Hexagonal), preservando la identidad de esta implementación de Onion frente a la comparación con las otras dos.
- **Separación Request / Dto**: cada operación tiene un `*Request` (HTTP, validado) en `presentation.request` y un `*Dto` (interno, puro) en `application.dto`, mapeados por `RestRequestMapper.toApplication(...)`.
- **`infrastructure.persistence.adapter`**: aunque Onion no usa el paquete `port`, sí nombra "adapter" a la implementación externa de un contrato — la regla 8 confirma que esto no contradice el vocabulario propio de la arquitectura, solo evita el término formal `port`.

---

## Conclusión: deficiencias y desventajas comparativas

**Clean Architecture**
Es la que más anillos formaliza (4: Entities, Use Cases, Interface Adapters, Frameworks & Drivers), lo que produce más *boilerplate* por operación que Onion (3 capas) — un `Request` HTTP, un `RequestModel` interno y su mapeo manual para cada caso de uso. Además, no todas las operaciones tienen un puerto de entrada explícito en `usecases.port.in`: varias (`RegistrarSiniestroUseCase`, `CrearTablaTarifariaUseCase`, `AutenticarUsuarioUseCase`, `RegistrarUsuarioUseCase`) son clases concretas sin interfaz propia, a diferencia de Hexagonal, donde **toda** operación de entrada tiene un `ports.in.*UseCase` verificado por ArchUnit (`inbound_ports_are_interfaces`). Esto significa que, en esos casos, Clean pierde parte de la inversión de dependencia hacia el consumidor que sí logra Hexagonal de forma uniforme.

**Arquitectura Hexagonal**
Es la que más paquetes y archivos requiere por funcionalidad (`ports/in`, `ports/out`, `adapters/inbound`, `adapters/outbound`, más el doble adaptador `memory`/`mongo` por repositorio), lo que eleva la curva de aprendizaje y el costo de navegación del código frente a Onion, que resuelve lo mismo con menos paquetes (`domain`, `application`, `infrastructure`, `presentation`). El vocabulario formal *port/adapter* tampoco distingue naturalmente entre reglas de negocio transversales (dominio) y reglas específicas de un caso de uso: todo vive bajo `core`, y solo la disciplina del equipo (reforzada aquí por `domain_does_not_depend_on_application_or_ports`) evita que la lógica de dominio y de aplicación se mezclen.

**Arquitectura Onion**
Es la menos prescriptiva sobre la forma del contrato de entrada: a diferencia de Hexagonal, no exige un puerto de entrada (`ports.in`) por cada operación, por lo que nada en el patrón en sí — más allá de la disciplina del equipo — impide que un caso de uso futuro se acople directamente a un repositorio sin pasar por un `*Dto` de comando explícito. También es la que menos separa formalmente *Domain Services* de *Application Services*: ambos terminan bajo `domain.service`/`application.service` sin una frontera reforzada por una regla ArchUnit tan granular como las de Hexagonal para `ports.in` vs `ports.out`. Finalmente, el uso de `infrastructure.persistence.adapter` (aunque justificado) puede generar confusión en quien conoce Hexagonal, al reutilizar la palabra "adapter" con un alcance distinto y sin el respaldo formal de un paquete `port`.
