# Architecture comparison -  Seguros

| Decision                    | Onion                              | Hexagonal                                               | Clean                                                                 |
| --------------------------- | ---------------------------------- | ------------------------------------------------------- | --------------------------------------------------------------------- |
| Physical form               | Single Maven module, package rings | Single Maven module, ports/adapters                     | Single Maven module, concentric Clean rings                           |
| Repository interfaces       | `domain.repository`              | `core.ports.out.persistence`                          | `usecases.port.out.repository`                                      |
| External service interfaces | `application.gateway`            | `core.ports.out`                                      | `usecases.port.out`                                                 |
| Business core               | `domain`                         | `core.domain` + `core.application` + `core.ports` | `entities` + `usecases`                                           |
| Enterprise business rules   | `domain`                         | `core.domain`                                         | `entities`                                                          |
| Application logic           | `application.service`            | `core.application.service`                            | `usecases.service`                                                  |
| REST entry                  | `presentation.controller`        | `adapters.inbound.rest.controller`                    | `interfaceadapters.in.rest.controller`                              |
| MongoDB                     | `infrastructure.persistence`     | `adapters.outbound.persistence.mongo`                 | `interfaceadapters.out.persistence.mongodb`                         |
| Framework/bootstrap         | `infrastructure` + root main     | `bootstrap`                                           | `frameworksdrivers.configuration` + `frameworksdrivers.bootstrap` |
| Enforcement                 | ArchUnit + packages                | ArchUnit + packages                                     | ArchUnit + strict concentric package boundaries                       |
