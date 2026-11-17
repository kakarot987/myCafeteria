# Angular 11's supported build-time Node runtime is Node 14.
FROM node:14.21.3-bullseye-slim AS build
WORKDIR /workspace
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/myCafeteriaAngular/ /usr/share/nginx/html/
EXPOSE 80
HEALTHCHECK --interval=15s --timeout=3s --start-period=15s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
