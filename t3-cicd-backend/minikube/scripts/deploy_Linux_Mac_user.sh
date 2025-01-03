#!/bin/bash

set -e
trap 'error "An unexpected error occurred. Exiting."' ERR

DB_NAMESPACE="t3cicdbackend-datastore"
BACKEND_NAMESPACE="t3cicdbackend"
CONFIG_IMAGE="wp161/t3cicdbackend-configservice:latest"
PIPELINE_IMAGE="wp161/t3cicdbackend-pipelineservice:latest"

GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
RESET='\033[0m'

function log() {
    echo -e "${CYAN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${RESET}"
}

function success() {
    echo -e "${GREEN}✔ $1${RESET}"
}

function warning() {
    echo -e "${YELLOW}⚠ $1${RESET}"
}

function error() {
    echo -e "${RED}✘ $1${RESET}"
}

# Function to check if Docker is running
function ensure_docker_running() {
    log "Checking if Docker is running..."

    # Determine the OS
    OS=$(uname -s)
    case "$OS" in
        Linux)
            if ! docker info &> /dev/null; then
                warning "Docker is not running. Attempting to start Docker on Linux..."
                sudo systemctl start docker
                log "Waiting for Docker to start..."
                while ! docker info &> /dev/null; do
                    sleep 2
                done
                success "Docker started successfully on Linux."
            else
                success "Docker is running on Linux."
            fi
            ;;
        Darwin)
            if ! docker info &> /dev/null; then
                warning "Docker is not running. Attempting to start Docker on macOS..."
                open --background -a Docker
                log "Waiting for Docker to start..."
                while ! docker info &> /dev/null; do
                    sleep 2
                done
                success "Docker started successfully on macOS."
            else
                success "Docker is running on macOS."
            fi
            ;;
        *)
            error "Unsupported OS: $OS. Unable to ensure Docker is running."
            exit 1
            ;;
    esac
}

# Function to check if Minikube is running
function ensure_minikube_running() {
    log "Checking if Minikube is running..."
    if ! minikube status &> /dev/null; then
        warning "Minikube is not running. Starting Minikube..."
        minikube start
    else
        success "Minikube is running."
    fi
    log "Setting Kubernetes context to Minikube..."
    kubectl config use-context minikube
    success "Kubernetes context set to Minikube."
}

# Function to check if a namespace exists
function ensure_namespace() {
    local namespace=$1
    log "Ensuring namespace $namespace exists..."
    if ! kubectl get namespace "$namespace" &> /dev/null; then
        warning "Namespace $namespace does not exist. Creating it..."
        kubectl create namespace "$namespace"
        success "Namespace $namespace created."
    else
        success "Namespace $namespace already exists."
    fi
}

# Function to wait for pods in a namespace to be in the desired state
function wait_for_pods() {
    local namespace=$1
    log "Waiting for all pods in namespace $namespace to be ready or completed..."

    while true; do
        # Check the status of all pods in the namespace
        pod_status=$(kubectl get pods -n "$namespace" --no-headers | awk '{print $3}')
        non_running=$(echo "$pod_status" | grep -v -E "Running|Completed" || true)

        if [[ -z "$non_running" ]]; then
            success "All pods in namespace $namespace are either Running or Completed."
            break
        else
            log "Some pods are not ready yet, this may take a while: $(echo "$pod_status" | grep -v -E "Running|Completed")"
            sleep 5
        fi
    done
}

# Function to wait for a specific pod to be ready
function wait_for_pod_ready() {
    local namespace=$1
    local pod_name_pattern=$2
    log "Waiting for pod matching pattern '$pod_name_pattern' in namespace '$namespace' to be ready..."
    while true; do
        # Check the status of the pod(s) matching the pattern
        pod_status=$(kubectl get pods -n "$namespace" | grep "$pod_name_pattern" | awk '{print $2}')
        if [[ "$pod_status" == "1/1" ]]; then
            success "Pod '$pod_name_pattern' in namespace '$namespace' is ready."
            break
        else
            log "Pod '$pod_name_pattern' is not ready yet. Waiting..."
            sleep 5
        fi
    done
}

# Function to wait for the webhook service to be ready
function wait_for_ingress_controller() {
    log "Waiting for Ingress Controller pod to be ready..."
    kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s
    success "Ingress Controller pod is ready."
}

# Start script
log "${CYAN}Ensuring Docker and Minikube are running...${RESET}"
ensure_docker_running
ensure_minikube_running

# Deploy DB Cluster
log "Starting ${CYAN}DB Cluster Deployment${RESET}..."
ensure_namespace "$DB_NAMESPACE"

log "Installing Multi-Cluster Services CRDs..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/mcs-api/refs/heads/master/config/crd/multicluster.x-k8s.io_serviceexports.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/mcs-api/refs/heads/master/config/crd/multicluster.x-k8s.io_serviceimports.yaml
success "Multi-Cluster Services CRDs installed successfully."

log "Applying DB YAML files..."
cd ..
kubectl apply -f postgres-secret.yaml -n "$DB_NAMESPACE"
kubectl apply -f postgres-configmap.yaml -n "$DB_NAMESPACE"
kubectl apply -f postgres-deploy.yaml -n "$DB_NAMESPACE"
kubectl apply -f pgadmin-secret.yaml -n "$DB_NAMESPACE"
kubectl apply -f pgadmin-deploy.yaml -n "$DB_NAMESPACE"

wait_for_pods "$DB_NAMESPACE"
success "DB Cluster Deployment Complete."

# Deploy Argo
log "Starting ${CYAN}Argo Deployment${RESET}..."
ensure_namespace "argo"

log "Deploying Argo Workflows..."
kubectl apply -f https://github.com/argoproj/argo-workflows/releases/download/v3.6.0/quick-start-minimal.yaml -n argo

wait_for_pods "argo"
success "Argo Deployment Complete."

# Deploy Backend Cluster
log "Starting ${CYAN}Backend Cluster Deployment${RESET}..."
ensure_namespace "$BACKEND_NAMESPACE"

log "Removing old images from Minikube..."
minikube ssh -- "docker rmi t3cicdbackend-configservice:latest --force || true" > /dev/null
minikube ssh -- "docker rmi t3cicdbackend-pipelineservice:latest --force || true" > /dev/null

log "Pulling images from Docker Hub..."
docker pull "$CONFIG_IMAGE"
docker pull "$PIPELINE_IMAGE"

log "Loading images into Minikube..."
minikube image load "$CONFIG_IMAGE"
minikube image load "$PIPELINE_IMAGE"

log "Tagging images without the prefix for Minikube..."
minikube ssh -- docker tag wp161/t3cicdbackend-configservice:latest t3cicdbackend-configservice:latest
minikube ssh -- docker tag wp161/t3cicdbackend-pipelineservice:latest t3cicdbackend-pipelineservice:latest

log "Applying Backend YAML files..."
kubectl apply -f postgres-secret.yaml -n "$BACKEND_NAMESPACE"
kubectl apply -f configservice-deployment.yaml -n "$BACKEND_NAMESPACE"
kubectl apply -f pipelineservice-deployment.yaml -n "$BACKEND_NAMESPACE"
kubectl apply -f clusterrolebinding.yaml -n t3cicdbackend

wait_for_pods "$BACKEND_NAMESPACE"
success "Backend Cluster Deployment Complete."

# Deploy Ingress Controller Locally
log "Enabling Minikube Ingress Addon..."
minikube addons enable ingress

wait_for_ingress_controller

log "Deploying Ingress Controller..."
kubectl apply -f ingress-controller-local.yaml -n "$BACKEND_NAMESPACE"

log "Waiting for Ingress Controller to be ready..."
wait_for_pod_ready "ingress-nginx" "ingress-nginx-controller"

success "Deployment Script Completed Successfully."
warning "Important post-deployment job:"
warning "If running on macOS, please run 'minikube tunnel' to expose ingress and allow traffic."