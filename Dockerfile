# Stage 1: Build frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build backend with frontend
FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app

# Copy backend source
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src

# Copy frontend build from previous stage
COPY --from=frontend-build /app/frontend/dist ./backend/src/main/resources/static

# Build the application
WORKDIR /app/backend
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar
COPY --from=backend-build /app/backend/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
