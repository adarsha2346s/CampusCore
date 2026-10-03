# ---------- build ----------
# Vite inlines VITE_* variables while building, so the public API root has to be
# supplied at build time. It must be a public URL only — never a secret.
FROM node:24-alpine AS build
WORKDIR /app

# Dependencies first so the install layer stays cached across source changes.
COPY package.json package-lock.json ./
RUN npm ci

ARG VITE_API_BASE_URL=http://localhost:8080/api/v1
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY . .
RUN npm run build

# ---------- serve ----------
# Serves the compiled single-page application. The container port is unchanged
# (5173) so the existing Compose port mapping and CORS defaults keep working.
FROM nginx:alpine AS serve
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 5173