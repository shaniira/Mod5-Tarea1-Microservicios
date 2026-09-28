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
| RabbitMQ en `CrashLoopBackOff` | `Error when reading /var/lib/rabbitmq/.erlang.cookie: eacces` | El entrypoint de la imagen oficial falla al hacer `chown` sobre el overlay de solo-imagen en el runtime de contenedores usado por kind/containerd | Se montó un `emptyDir` en `/var/lib/rabbitmq`. **Fase 7:** reemplazado por un `StatefulSet` con volumen persistente (`volumeClaimTemplates`): el directorio del volumen también es escribible y, además, colas y mensajes sobreviven a un reinicio del Pod (con `emptyDir` se perdían) |
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
- Las imágenes `andina-api-gateway:1.0.0`, `andina-seguros-clean:1.0.0`, `customer-service:1.0.0`,
  `claims-service:1.0.0`, `quotation-service:1.0.0` y `policy-service:1.0.0` construidas y
  disponibles para el clúster (`docker build` + push a un registro, o `kind load docker-image` /
  `minikube image load` en un clúster local).
- `kubectl` apuntando al clúster correcto.

## Orden de aplicación

```bash
kubectl apply -f k8s/00-namespace.yaml

# Secretos reales primero (nunca los *.example.yaml tal cual). Fase 2: la clave privada RS256
# de los JWT solo la monta identity-service; el gateway y los servicios usan la clave publica (JWKS).
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private.pem
kubectl create secret generic identity-jwt-key --namespace andina-seguros \
  --from-file=private.pem=private.pem && rm private.pem
# repetir el patrón con mongodb-secrets (33-secret-mongodb.example.yaml), identity-secrets
# (51), notification-secrets (61) y rabbitmq-secrets (31).
# MongoDB usa los mismos scripts que Docker Compose (replica set + usuarios por servicio):
kubectl create configmap mongodb-scripts --namespace andina-seguros --from-file=infra/mongo/

kubectl apply -f k8s/11-configmap-gateway.yaml
kubectl apply -f k8s/12-deployment-gateway.yaml
kubectl apply -f k8s/13-service-gateway.yaml
kubectl apply -f k8s/14-hpa-gateway.yaml

# Paso 6.10: el monolito (backend) ya no se despliega. Sus manifiestos quedan archivados en
# k8s/archivo-monolito/ (20-23) solo como referencia.

kubectl apply -f k8s/30-mongodb.yaml
kubectl apply -f k8s/31-rabbitmq.yaml
kubectl apply -f k8s/32-redis.yaml

# Fase 2: identity-service (2 réplicas)
kubectl apply -f k8s/50-configmap-identity.yaml
kubectl apply -f k8s/52-deployment-identity.yaml
kubectl apply -f k8s/53-service-identity.yaml

# Fase 1: notification-service (sin Service: solo consume RabbitMQ)
kubectl apply -f k8s/60-configmap-notification.yaml
kubectl apply -f k8s/62-deployment-notification.yaml

# Fases 3 a 6: customer, claims, quotation y policy (antes, sus Secrets: 71, 81, 91 y 96).
# El gateway (11) ya apunta a ellos.
for s in 70-configmap-customer 72-deployment-customer 73-service-customer \
         80-configmap-claims 82-deployment-claims 83-service-claims \
         90-configmap-quotation 92-deployment-quotation 93-service-quotation \
         95-configmap-policy 97-deployment-policy 98-service-policy; do
  kubectl apply -f "k8s/$s.yaml"
done

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
| **backend sin HPA** (archivado en el paso 6.10) | No se define `HorizontalPodAutoscaler` para `backend` y su `Deployment` queda en `replicas: 1` | Desde la fase 2 el estado efímero (OAuth, tickets, MFA) está en Redis dentro de identity-service, pero el relay del Outbox del backend está pensado para una sola réplica. Se habilita el escalado cuando el relay reclame cada evento antes de enviarlo. |
| **identity-service con 2 réplicas** | `replicas: 2`, sin HPA todavía | No guarda estado en memoria (Redis), así que escala sin fallos de MFA ni Facebook (probado en Docker Compose, paso 2.10). Usa la base `identity_db` del MongoDB del clúster con su propio usuario (solo `readWrite` sobre esa base). |
| **api-gateway con HPA y 2 réplicas mínimo** | `minReplicas: 2`, CPU 70% / memoria 80% | El Gateway no guarda estado propio (el rate limiter vive en Redis), así que sí es seguro escalarlo horizontalmente desde ya. Cumple la regla "API Gateway: mínimo 2 réplicas en ambientes no locales". |
| **MongoDB y RabbitMQ de un solo Pod** | `replicas: 1`, sin clustering | MongoDB es un replica set **de un nodo** (necesario para las transacciones del Outbox) con autenticación obligatoria y un usuario por servicio (`andina`, `identity`, `notification`), cada uno solo con permiso sobre su base. El Service es headless para que el nombre del replica set resuelva a la IP del Pod. Alta disponibilidad real (3 nodos) no se implementó en la fase 7: queda documentada como evolución (ver `n_…`, sección 5). RabbitMQ pasó a `StatefulSet` con volumen persistente. |
| **JWT RS256 con JWKS (fase 2)** | Solo `identity-service` monta el `Secret` `identity-jwt-key`; `api-gateway` y `backend` descargan la clave pública de `/.well-known/jwks.json` | Se retiró el secreto simétrico compartido (`andina-jwt-secret`): robar el gateway o el backend ya no permite firmar tokens. |
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
