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
COPY nginx.conf /etc/nginx/conf.d/default.conf
#use custom config
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
