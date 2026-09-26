# Manifiestos Kubernetes — Andina Seguros (Fase 0: Gateway delante del monolito)

Estos manifiestos son el **objetivo de orquestación** (ver "DOCKER COMPOSE" y "OBJETIVO FINAL"
en la propuesta de migración): Docker Compose sigue siendo lo que se usa para desarrollo local
(`Arquitectura-Clean/docker-compose.yml`); estos YAML son para un clúster real (minikube, kind,
k3d, EKS/GKE/AKS, etc.).

**Estado de validación: desplegado y probado de punta a punta en un clúster real.** Como este
entorno de desarrollo no traía ningún clúster (ni Docker Desktop Kubernetes, ni minikube, ni
kind preinstalados), se creó uno local con **kind** (usa contenedores Docker, sin necesidad de
GUI) para no quedarse solo en la validación de sintaxis:

```bash
kind create cluster --name andina-seguros
kind load docker-image andina-api-gateway:1.0.0 --name andina-seguros
kind load docker-image andina-seguros-clean:1.0.0 --name andina-seguros
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl apply -f k8s/
```

Resultado verificado con tráfico real (no solo `kubectl get`):

```
curl -k -H "Host: andina-seguros.example.com" https://localhost/api/auth/login -X POST ...
-> HTTP 308 en :80 (ssl-redirect funcionando) -> HTTPS en :443 -> Ingress -> api-gateway-service
   (balanceando entre 2 Pods) -> backend-service -> backend -> MongoDB -> JWT real devuelto
```

Es decir, se probó la cadena completa `Ingress → API Gateway → Backend → MongoDB` tal como la
pide el diagrama objetivo de la fase 0, con los 15 manifiestos de este directorio aplicados sin
modificar nada a mano salvo crear los `Secret` reales (ver más abajo).

### Dos errores reales que solo aparecieron al desplegar (no al leer el YAML)

| Error | Sintoma | Causa | Corrección |
|---|---|---|---|
| RabbitMQ en `CrashLoopBackOff` | `Error when reading /var/lib/rabbitmq/.erlang.cookie: eacces` | El entrypoint de la imagen oficial falla al hacer `chown` sobre el overlay de solo-imagen en el runtime de contenedores usado por kind/containerd | Se montó un `emptyDir` en `/var/lib/rabbitmq` (ver `31-rabbitmq.yaml`) |
| RabbitMQ y MongoDB reiniciando en bucle después de arrancar bien | `Liveness probe failed: command timed out... after 1s` | Los *probes* con `exec` (`rabbitmq-diagnostics`, `mongosh`) no tenían `timeoutSeconds` explícito; el valor por defecto de Kubernetes es **1 segundo**, y esos comandos arrancan su propio proceso/nodo cada vez que se invocan — bajo la CPU limitada de una máquina de desarrollo corriendo todo el stack a la vez, tardan más que eso | Se agregó `timeoutSeconds: 10` (5 en los `httpGet` del gateway/backend, más baratos de ejecutar) y un `startupProbe` propio a RabbitMQ y MongoDB en vez de dejar que el `livenessProbe` los matara mientras aún estaban arrancando |

### Limitación encontrada: el HPA no puede escalar sin `metrics-server`

`kubectl get hpa` mostró `cpu: <unknown>/70%` — **kind no trae `metrics-server` instalado por
defecto**, y sin él el `HorizontalPodAutoscaler` no tiene de dónde leer el uso de CPU/memoria
(el objeto se crea igual, simplemente no escala). La mayoría de los clústeres gestionados (EKS,
GKE, AKS) sí lo traen; en kind o en un clúster on-premise nuevo hay que instalarlo aparte:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
# en kind, además, hay que tolerar el certificado interno del kubelet:
kubectl patch deployment metrics-server -n kube-system --type='json' \
  -p='[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
```

No se instaló en esta prueba porque no es parte de los manifiestos de la aplicación (es
infraestructura del clúster, igual que el Ingress Controller); queda documentado aquí para quien
despliegue esto en un clúster nuevo.

### Cómo reproducirlo (o limpiarlo)

```bash
kubectl get pods -n andina-seguros -o wide     # ver el estado actual
kubectl port-forward -n andina-seguros svc/api-gateway-service 18080:8080  # probar sin Ingress

kind delete cluster --name andina-seguros      # borrar todo el clúster de prueba
```

## Requisitos previos

- Un clúster con **NGINX Ingress Controller** instalado (ver comentario en `40-ingress.yaml`).
- Las imágenes `andina-api-gateway:1.0.0` y `andina-seguros-clean:1.0.0` construidas y
  disponibles para el clúster (`docker build` + push a un registro, o `kind load docker-image` /
  `minikube image load` en un clúster local).
- `kubectl` apuntando al clúster correcto.

## Orden de aplicación

```bash
kubectl apply -f k8s/00-namespace.yaml

# Secretos reales primero (nunca los *.example.yaml tal cual). Ejemplo con el JWT:
kubectl create secret generic andina-jwt-secret --namespace andina-seguros \
  --from-literal=JWT_SECRET="$(openssl rand -base64 48)"
# repetir el patrón con backend-secrets (21-secret-backend.example.yaml) y
# rabbitmq-secrets (31-secret-rabbitmq.example.yaml)

kubectl apply -f k8s/11-configmap-gateway.yaml
kubectl apply -f k8s/12-deployment-gateway.yaml
kubectl apply -f k8s/13-service-gateway.yaml
kubectl apply -f k8s/14-hpa-gateway.yaml

kubectl apply -f k8s/20-configmap-backend.yaml
kubectl apply -f k8s/22-deployment-backend.yaml
kubectl apply -f k8s/23-service-backend.yaml

kubectl apply -f k8s/30-mongodb.yaml
kubectl apply -f k8s/31-rabbitmq.yaml
kubectl apply -f k8s/32-redis.yaml

kubectl apply -f k8s/40-ingress.yaml
```

Verificar:

```bash
kubectl get pods,svc,hpa,ingress -n andina-seguros
kubectl logs -n andina-seguros deploy/api-gateway -f
```

## Decisiones y limitaciones (para no generar falsas expectativas)

| Punto | Decisión | Por qué |
|---|---|---|
| **backend sin HPA** | No se define `HorizontalPodAutoscaler` para `backend` y su `Deployment` queda en `replicas: 1` | El backend todavía guarda estado efímero en memoria (OAuth state de Facebook, tickets de login, desafíos MFA — riesgo A3). Con 2+ réplicas, ese estado no se comparte y fallan intermitentemente Facebook y MFA. Se habilita el escalado cuando ese estado se mueva a Redis (fase 2, identity-service). Escalar el backend hoy con un HPA sería mentir sobre una capacidad que el código no soporta. |
| **api-gateway con HPA y 2 réplicas mínimo** | `minReplicas: 2`, CPU 70% / memoria 80% | El Gateway no guarda estado propio (el rate limiter vive en Redis), así que sí es seguro escalarlo horizontalmente desde ya. Cumple la regla "API Gateway: mínimo 2 réplicas en ambientes no locales". |
| **MongoDB y RabbitMQ de un solo Pod** | `replicas: 1`, sin clustering | Igual que hoy en Docker Compose: son infraestructura compartida de la fase 0, no el foco de esta entrega. Alta disponibilidad de datos queda para la fase 7 (endurecimiento) del plan de migración. |
| **JWT compartido (HS256)** | Un único `Secret` (`andina-jwt-secret`) leído tanto por `api-gateway` como por `backend` | Ver `gateway/src/main/java/com/andinaseguros/gateway/security/JwtValidator.java`: hasta que exista `identity-service` con JWKS (RS256, fase 2), ambos deben validar el mismo secreto simétrico. |
| **Sin TLS real configurado** | El `Ingress` referencia `andina-seguros-tls` y `letsencrypt-prod`, pero ninguno existe todavía | Son placeholders de ejemplo; instalar cert-manager (o cargar un certificado propio) es un paso de entorno, no de este repositorio. |
| **Sin Eureka / service discovery adicional** | Se usa el DNS interno de Kubernetes (`<service>.<namespace>.svc.cluster.local`) | Cumple la regla "no introducir Eureka salvo necesidad real"; el DNS de Kubernetes ya resuelve el problema. |

## Relación con Docker Compose

| Docker Compose (`Arquitectura-Clean/docker-compose.yml`) | Kubernetes (aquí) |
|---|---|
| `environment:` con valores literales | `ConfigMap` (no sensible) + `Secret` (sensible) |
| Nombre del servicio en la red de Compose (`http://backend:8080`) | Nombre del `Service` + DNS de Kubernetes (`http://backend-service.andina-seguros.svc.cluster.local:8080`) |
| `depends_on` + `healthcheck` | `startupProbe` / `readinessProbe` / `livenessProbe` |
| `restart: unless-stopped` | `strategy: RollingUpdate` + reinicio automático de Pods por el `Deployment` |
| Un solo host, sin autoescalado | `HorizontalPodAutoscaler` (gateway) |
