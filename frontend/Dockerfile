# ============================================================
# Etapa 1: compilación Vue con pnpm
# Se usa pnpm para evitar el fallo de npm "Exit handler never called"
# observado durante instalaciones lentas dentro de Docker Desktop.
# ============================================================
FROM node:24-bookworm-slim AS build

WORKDIR /app

ENV NODE_ENV=development \
    CI=true \
    PNPM_HOME=/pnpm \
    PATH=/pnpm:$PATH

# Corepack viene incluido con Node. Fijamos una versión estable de pnpm.
RUN corepack enable \
    && corepack prepare pnpm@9.15.9 --activate \
    && pnpm config set fetch-retries 10 \
    && pnpm config set fetch-retry-factor 2 \
    && pnpm config set fetch-retry-mintimeout 20000 \
    && pnpm config set fetch-retry-maxtimeout 180000 \
    && pnpm --version

COPY package.json ./

# Instala dependencias normales y de desarrollo necesarias para vue-tsc/Vite.
# No se usa frozen-lockfile porque el proyecto original no incluye pnpm-lock.yaml.
RUN pnpm install \
      --no-frozen-lockfile \
      --prod=false \
      --reporter=append-only \
    && test -x node_modules/.bin/vue-tsc \
    && test -x node_modules/.bin/vite \
    && ./node_modules/.bin/vue-tsc --version \
    && ./node_modules/.bin/vite --version

COPY . .

ARG VITE_API_URL=http://localhost:8083/api
ENV VITE_API_URL=${VITE_API_URL}
ARG VITE_GOOGLE_CLIENT_ID
ENV VITE_GOOGLE_CLIENT_ID=${VITE_GOOGLE_CLIENT_ID}

RUN pnpm run test \
    && pnpm run build

# ============================================================
# Etapa 2: Nginx
# ============================================================
FROM nginx:1.27-alpine AS runtime

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
