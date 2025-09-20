# Stage 1: Build React app with Vite
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build
RUN ls -R /app   # Debug: shows us that /app/build exists

# Stage 2: Serve with nginx
FROM nginx:stable-alpine

WORKDIR /usr/share/nginx/html
RUN rm -rf ./*
COPY --from=build /app/build/ .

# Add custom nginx config template (with $PORT placeholder)
RUN printf 'server {\n\
    listen       ${PORT};\n\
    server_name  localhost;\n\
    root   /usr/share/nginx/html;\n\
    index  index.html;\n\
    location / {\n\
      try_files $uri /index.html;\n\
    }\n\
}' > /etc/nginx/conf.d/default.conf.template

# At runtime, replace ${PORT} with actual $PORT
CMD sh -c "envsubst '\$PORT' < /etc/nginx/conf.d/default.conf.template > /etc/nginx/conf.d/default.conf && exec nginx -g 'daemon off;'"

EXPOSE 8080
