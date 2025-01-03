# Stage 1: Build Stage
FROM eclipse-temurin:21 AS build

WORKDIR /app

# Copy necessary files from the root
COPY ../gradlew ./gradlew
COPY ../gradle ./gradle
COPY ../settings.gradle.kts ./

# Copy the subproject into the container
COPY ./pipelineservice/ ./pipelineservice/
COPY ./shared/ ./shared/

# Build only the subproject
RUN ./gradlew :pipelineservice:bootJar

# Stage 2: Runtime Stage
FROM eclipse-temurin:21

WORKDIR /app

# Install dependencies and tools
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    sudo \
    passwd \
    util-linux \
    apt-transport-https \
    conntrack \
    socat \
    iptables \
    gnupg \
    ca-certificates \
    lsb-release \
    && rm -rf /var/lib/apt/lists/*

# Install Argo CLI
RUN curl -sLO "https://github.com/argoproj/argo-workflows/releases/download/v3.6.0/argo-linux-amd64.gz" \
    && gunzip "argo-linux-amd64.gz" \
    && chmod +x "argo-linux-amd64" \
    && mv "./argo-linux-amd64" /usr/local/bin/argo

# Install Docker CE (Docker Desktop CLI features can work in conjunction with Docker Engine)
RUN mkdir -p /etc/apt/keyrings && \
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg && \
    echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" \
    > /etc/apt/sources.list.d/docker.list && \
    apt-get update && apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin && \
    rm -rf /var/lib/apt/lists/*

# Install kubectl
RUN curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl" && \
    chmod +x kubectl && \
    mv kubectl /usr/local/bin/

# Set the Kubernetes master URL
ENV KUBERNETES_MASTER=http://192.168.49.2:8443

# Set the KUBECONFIG environment variable for Argo CLI (optional)
ENV KUBECONFIG=/dev/null

# Expose Minikube Docker daemon
ENV DOCKER_HOST=unix:///var/run/docker.sock

# Verify installations
RUN argo version 
RUN docker --version

# Copy the built JAR from the build stage
COPY --from=build /app/pipelineservice/build/libs/*.jar ./app.jar

EXPOSE 8080

# Set the default command to run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
