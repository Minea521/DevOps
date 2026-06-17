# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Nginx and SSH
RUN apk add --no-cache nginx openssh-server && \
    mkdir -p /run/nginx && \
    echo "root:Hello@123" | chpasswd

# Copy JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Configure Nginx
RUN echo 'server { \
    listen 8080; \
    location / { \
        proxy_pass http://localhost:8081; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
}' > /etc/nginx/http.d/default.conf

# Configure SSH
RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config

# Create startup script
RUN echo '#!/bin/sh\n\
nginx\n\
service sshd start\n\
exec java -jar /app/app.jar --server.port=8081\n\
' > /start.sh && chmod +x /start.sh

EXPOSE 8080 22

CMD ["/start.sh"]
# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Nginx and SSH
RUN apk add --no-cache nginx openssh-server && \
    mkdir -p /run/nginx && \
    echo "root:Hello@123" | chpasswd

# Copy JAR file from build stage
COPY --from=build /app/target/*.jar app.jar

# Configure Nginx
RUN echo 'server { \
    listen 8080; \
    location / { \
        proxy_pass http://localhost:8081; \
        proxy_set_header Host $host; \
        proxy_set_header X-Real-IP $remote_addr; \
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; \
        proxy_set_header X-Forwarded-Proto $scheme; \
    } \
}' > /etc/nginx/http.d/default.conf

# Configure SSH
RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config

# Create startup script - FIXED VERSION
RUN echo '#!/bin/sh' > /start.sh && \
    echo 'nginx' >> /start.sh && \
    echo 'service sshd start' >> /start.sh && \
    echo 'exec java -jar /app/app.jar --server.port=8081' >> /start.sh && \
    chmod +x /start.sh

EXPOSE 8080 22

CMD ["/start.sh"]
