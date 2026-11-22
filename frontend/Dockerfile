# Contêiner do FRONTEND (TypeScript compilado e servido pelo nginx).
FROM node:22-alpine AS build
WORKDIR /frontend
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /frontend/index.html /frontend/styles.css /usr/share/nginx/html/
COPY --from=build /frontend/dist /usr/share/nginx/html/dist
EXPOSE 80
