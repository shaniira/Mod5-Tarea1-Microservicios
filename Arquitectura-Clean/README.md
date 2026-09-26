# Andina Seguros — Clean Architecture

Proyecto Maven monomódulo con un único `src`, organizado por distancia al negocio mediante anillos concéntricos.

```text
com.andinaseguros/
├── entities/                    # Enterprise Business Rules
├── usecases/
│   ├── port/in/                  # entrada a casos de uso
│   ├── port/out/                 # necesidades del núcleo
│   ├── dto/                      # un archivo por Request Model (p. ej. CrearClienteRequestModel) + Responses,
│   │                              # sin anotaciones de framework
│   └── service/                  # Application Business Rules
├── interfaceadapters/
│   ├── in/rest/
│   │   ├── controller/            # controllers, traducción HTTP
│   │   ├── request/                # un archivo por Request HTTP (p. ej. CrearClienteRequest) con validación (jakarta.validation)
│   │   ├── mapper/                 # RestRequestMapper: *Request -> *RequestModel
│   │   ├── response/               # DTOs de salida HTTP
│   │   └── exception/              # manejo de errores HTTP
│   └── out/                      # gateways MongoDB, APIs, notificación y seguridad
└── frameworksdrivers/
    ├── configuration/            # Spring, Mongo, OpenAPI y Security
    └── bootstrap/                # composition root
```

Regla de dependencias: `frameworksdrivers → interfaceadapters → usecases → entities`. Los puertos de salida pertenecen a `usecases.port.out`; las implementaciones externas pertenecen a `interfaceadapters.out`.

Clean se diferencia de Hexagonal por el lenguaje explícito de anillos: Entities, Use Cases, Interface Adapters y Frameworks & Drivers. `CleanArchitectureTest` (ArchUnit) impide dependencias desde anillos internos hacia anillos externos, y además prohíbe que `usecases` dependa de frameworks (`org.springframework..`, `jakarta.persistence..`, `jakarta.validation..`, `org.bson..`, `com.mongodb..`).

## Validación de entrada HTTP

Siguiendo el vocabulario de *Clean Architecture* (Robert C. Martin), el DTO de entrada de cada caso de uso se llama **Request Model** (p. ej. `CrearClienteRequestModel`, `EmitirPolizaRequestModel`) y vive en `usecases.dto`, uno por operación — son POJOs puros, sin anotaciones `jakarta.validation`, porque los casos de uso no deben depender de una API de framework.

La validación de las peticiones HTTP vive exclusivamente en `interfaceadapters.in.rest.request`, con un **Request** por operación (p. ej. `CrearClienteRequest`, anotado con `jakarta.validation.constraints`). Los controllers reciben el `*Request` con `@Valid` y lo convierten al `*RequestModel` correspondiente mediante `RestRequestMapper.toCore(...)` antes de invocar el puerto de entrada. Mantener nombres distintos entre ambas capas (`Request` vs `RequestModel`) evita la ambigüedad de tener el mismo nombre de clase en dos paquetes distintos.
