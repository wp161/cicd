# Enable strict mode to catch errors
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$DB_NAMESPACE = "t3cicdbackend-datastore"
$BACKEND_NAMESPACE = "t3cicdbackend"
$CONFIG_IMAGE = "t3cicdbackend-configservice:latest"
$PIPELINE_IMAGE = "t3cicdbackend-pipelineservice:latest"

# Logging functions
function Log {
    param (
        [string]$Message
    )
    Write-Host "[$(Get-Date -Format "yyyy-MM-dd HH:mm:ss")] $Message" -ForegroundColor Cyan
}

function Success {
    param (
        [string]$Message
    )
    Write-Host "$Message" -ForegroundColor Green
}

function Warning {
    param (
        [string]$Message
    )
    Write-Host "$Message" -ForegroundColor Yellow
}

function ErrorLog {
    param (
        [string]$Message
    )
    Write-Host "$Message" -ForegroundColor Red
}

# Function to check if Docker is running
function Ensure-DockerRunning {
    Log "Checking if Docker is running..."
    docker info | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Success "Docker is running."
    } else {
        Warning "Docker is not running. Please start Docker Desktop."
        throw "Docker is not running."
    }
}


# Function to check if Minikube is running
function Ensure-MinikubeRunning {
    Log "Checking if Minikube is running..."
    minikube status | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Success "Minikube is running."
    } else {
        Warning "Minikube is not running. Starting Minikube..."
        minikube start | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Success "Minikube started successfully."
        } else {
            ErrorLog "Failed to start Minikube."
            throw "Failed to start Minikube."
        }
    }
    Log "Setting Kubernetes context to Minikube..."
    kubectl config use-context minikube | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Success "Kubernetes context set to Minikube."
    } else {
        ErrorLog "Failed to set Kubernetes context to Minikube."
        throw "Failed to set Kubernetes context to Minikube."
    }
}


# Function to ensure namespace exists
function Ensure-Namespace {
    param (
        [string]$Namespace
    )
    Log "Ensuring namespace $Namespace exists..."
    kubectl get namespace $Namespace | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Success "Namespace $Namespace already exists."
    } else {
        Warning "Namespace $Namespace does not exist. Creating it..."
        kubectl create namespace $Namespace | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Success "Namespace $Namespace created."
        } else {
            ErrorLog "Failed to create namespace $Namespace."
            throw "Failed to create namespace $Namespace."
        }
    }
}


# Function to wait for pods in a namespace to be in the desired state
function Wait-ForPods {
    param (
        [string]$Namespace
    )
    Log "Waiting for all pods in namespace '$Namespace' to be ready or completed..."
    while ($true) {
        # Get the status of all pods in the namespace
        $PodStatus = kubectl get pods -n $Namespace --no-headers | ForEach-Object { ($_ -split '\s+')[2] }
        $NonRunning = $PodStatus | Where-Object { $_ -notmatch "Running|Completed" }


        if (-not $NonRunning) {
            Success "All pods in namespace '$Namespace' are either Running or Completed."
            break
        } else {
            Log "Some pods are not ready yet, this may take a while: $($NonRunning -join ', ')"
            Start-Sleep -Seconds 5
        }
    }
}


# Function to wait for a specific pod to be ready
function Wait-ForPodReady {
    param (
        [string]$Namespace,
        [string]$PodNamePattern
    )
    Log "Waiting for pod matching pattern '$PodNamePattern' in namespace '$Namespace' to be ready..."
    while ($true) {
        $PodStatus = kubectl get pods -n $Namespace | Select-String -Pattern $PodNamePattern | ForEach-Object { $_ -split '\s+' } | Where-Object { $_ -match "1/1" }
        if ($PodStatus) {
            Success "Pod '$PodNamePattern' in namespace '$Namespace' is ready."
            break
        } else {
            Log "Pod '$PodNamePattern' is not ready yet. Waiting..."
            Start-Sleep -Seconds 5
        }
    }
}


# Function to wait for the webhook service to be ready
function Wait-ForIngressController {
    Log "Waiting for Ingress Controller pod to be ready..."
    try {
        kubectl wait --namespace ingress-nginx --for=condition=ready pod --selector=app.kubernetes.io/component=controller --timeout=300s | Out-Null
        Success "Ingress Controller pod is ready."
    } catch {
        ErrorLog "Failed to wait for Ingress Controller pod to be ready. Ensure the pod is running and healthy."
        throw "Ingress Controller pod readiness timed out or failed."
    }
}


# Start the script
Log "Ensuring Docker and Minikube are running..."
Ensure-DockerRunning
Ensure-MinikubeRunning


# Deploy DB Cluster
Log "Starting DB Cluster Deployment..."
Ensure-Namespace $DB_NAMESPACE


Log "Installing Multi-Cluster Services CRDs..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/mcs-api/master/config/crd/multicluster.x-k8s.io_serviceimports.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/mcs-api/master/config/crd/multicluster.x-k8s.io_serviceexports.yaml
Success "Multi-Cluster Services CRDs installed successfully."




Log "Applying DB YAML files..."
Set-Location ..
kubectl apply -f postgres-secret.yaml -n $DB_NAMESPACE | Out-Null
kubectl apply -f postgres-configmap.yaml -n $DB_NAMESPACE | Out-Null
kubectl apply -f postgres-deploy.yaml -n $DB_NAMESPACE | Out-Null
kubectl apply -f pgadmin-secret.yaml -n $DB_NAMESPACE | Out-Null
kubectl apply -f pgadmin-deploy.yaml -n $DB_NAMESPACE | Out-Null
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to apply DB YAML files."
    throw "DB YAML application failed."
}
Wait-ForPods $DB_NAMESPACE
Success "DB Cluster Deployment Complete."


# Deploy Argo
Log "Starting Argo Deployment..."
Ensure-Namespace "argo"
Log "Deploying Argo Workflows..."
kubectl apply -f https://github.com/argoproj/argo-workflows/releases/download/v3.6.0/quick-start-minimal.yaml -n argo
Wait-ForPods "argo"
Success "Argo Deployment Complete."


# Deploy Backend Cluster
Log "Starting Backend Cluster Deployment..."
Ensure-Namespace $BACKEND_NAMESPACE


Log "Removing old images from Minikube..."
minikube ssh "docker rmi $CONFIG_IMAGE --force" | Out-Null
minikube ssh "docker rmi $PIPELINE_IMAGE --force" | Out-Null


Log "Building Docker Images..."
Set-Location ..
./gradlew wrapper
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to set up Gradle wrapper."
    throw "Gradle wrapper setup failed."
}
docker build -t $CONFIG_IMAGE -f configservice/Dockerfile .
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to build config service image."
    throw "Config service image build failed."
}
docker build -t $PIPELINE_IMAGE -f pipelineservice/Dockerfile .
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to build pipeline service image."
    throw "Pipeline service image build failed."
}


Log "Loading Images into Minikube..."
Log "Loading Image $CONFIG_IMAGE to Minikube.."
minikube image load $CONFIG_IMAGE | Out-Null
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to load $CONFIG_IMAGE into Minikube."
    throw "Image loading failed for $CONFIG_IMAGE."
}


Log "Loading Image $PIPELINE_IMAGE to Minikube.."
minikube image load $PIPELINE_IMAGE | Out-Null
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to load $PIPELINE_IMAGE into Minikube."
    throw "Image loading failed for $PIPELINE_IMAGE."
}


Log "Applying Backend YAML files..."
Set-Location minikube
kubectl apply -f postgres-secret.yaml -n $BACKEND_NAMESPACE | Out-Null
kubectl apply -f configservice-deployment.yaml -n $BACKEND_NAMESPACE | Out-Null
kubectl apply -f pipelineservice-deployment.yaml -n $BACKEND_NAMESPACE | Out-Null
kubectl apply -f clusterrolebinding.yaml -n $BACKEND_NAMESPACE | Out-Null
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to apply Backend YAML files."
    throw "Backend YAML application failed."
}
Wait-ForPods $BACKEND_NAMESPACE
Success "Backend Cluster Deployment Complete."


# Deploy Ingress Controller Locally
Log "Enabling Minikube Ingress Addon..."
minikube addons enable ingress | Out-Null
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to enable Minikube ingress addon."
    throw "Minikube ingress addon enabling failed."
}


Wait-ForIngressController


Log "Deploying Ingress Controller..."
kubectl apply -f ingress-controller-local.yaml -n $BACKEND_NAMESPACE | Out-Null
if ($LASTEXITCODE -ne 0) {
    ErrorLog "Failed to deploy Ingress Controller."
    throw "Ingress Controller deployment failed."
}


Log "Waiting for Ingress Controller to be ready..."
Wait-ForPodReady "ingress-nginx" "ingress-nginx-controller"


Success "Deployment Script Completed Successfully."
Warning "Important post-deployment job:"
Warning "If running on Windows, please run 'minikube tunnel' to expose ingress and allow traffic."
