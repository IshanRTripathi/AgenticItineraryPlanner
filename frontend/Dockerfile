# Stage 1: Build React app with Vite
FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Stage 2: Serve with nginx
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html
# Remove default nginx html
RUN rm -rf ./*
# Copy the Vite build output
COPY --from=build /app/build .    # ✅ uses "build" because outDir=build in vite.config.js
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
