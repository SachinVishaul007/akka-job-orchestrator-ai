@echo off
echo Deleting Kubernetes resources...
kubectl delete -f k8s/service.yaml
kubectl delete -f k8s/deployment.yaml

echo Stopping Minikube...
minikube stop

echo "Cleanup complete!"
