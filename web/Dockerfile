# ============================================================
# Lab 7 - Dockerfile for the WEB service (static frontend)
# Single stage: the official nginx image already contains a web
# server, we only drop in our page and our proxy configuration.
# Build context: ./web  (see docker-compose.yml)
# ============================================================

# FROM: official nginx image, runs the web server on port 80.
FROM nginx:1.27-alpine

# WORKDIR: not strictly required, kept for readability.
WORKDIR /usr/share/nginx/html

# COPY: our reverse-proxy config replaces the default nginx site.
# It forwards /api/ to the compose service name "backend" (port 8000).
COPY nginx.conf /etc/nginx/conf.d/default.conf

# COPY: the static calculator page served at http://127.0.0.1:8080.
COPY index.html /usr/share/nginx/html/index.html

# EXPOSE: documents container port 80; compose maps host 8080 -> 80.
EXPOSE 80

# CMD: inherited from the nginx base image (nginx -g 'daemon off;'),
# so no override is needed here.
