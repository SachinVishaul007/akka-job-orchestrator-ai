@echo off
echo Starting Minikube...
minikube start

REM Set docker env
echo Configuring Docker to use Minikube's Docker daemon...
FOR /f "tokens=*" %%i IN ('minikube docker-env --shell cmd') DO %%i

REM Build the Docker image
echo Building Docker image...
docker build -t resume-app .

REM Enable ingress addon
echo Enabling Ingress addon...
minikube addons enable ingress

REM Apply Kubernetes manifests
echo Applying Kubernetes manifests...
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

echo Waiting for pods to be ready...
kubectl wait --for=condition=ready pod -l app=resume-app --timeout=120s

echo.
echo Setup complete!
echo To access the application, run: minikube service resume-service
echo Or access via the ingress with: minikube ip