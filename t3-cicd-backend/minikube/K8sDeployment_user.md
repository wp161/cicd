## Table of Contents
- [Deploy DB Cluster](#deploy-backend-cluster-t3cicdbackend)
- [Deploy Backend Cluster](#deploy-backend-cluster-t3cicdbackend)
  - [Deploy ConfigService](#deploy-configservice)
  - [Deploy Ingress Controller Locally](#deploy-ingress-controller-locally)
- [Update Backend Cluster after Code Change](#update-backend-cluster-after-code-change)
## Deploy DB Cluster (t3cicdbackend-datastore)
1. Install Minikube
2. Start Minikube
```bash
minikube start 
```
3. Create namespace
```bash
kubectl create namespace t3cicdbackend-datastore
```
4. Install CRD for multi-cluster communication
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/mcs-api/refs/heads/master/config/crd/multicluster.x-k8s.io_serviceexports.yaml
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/mcs-api/refs/heads/master/config/crd/multicluster.x-k8s.io_serviceimports.yaml
```
5. Apply all PostgreSQL and PgAdmin YAML files
```bash
kubectl apply -f postgres-secret.yaml -n t3cicdbackend-datastore
kubectl apply -f postgres-configmap.yaml -n t3cicdbackend-datastore
kubectl apply -f postgres-deploy.yaml -n t3cicdbackend-datastore
kubectl apply -f pgadmin-secret.yaml -n t3cicdbackend-datastore
kubectl apply -f pgadmin-deploy.yaml -n t3cicdbackend-datastore
```

6. Verify
```bash
kubectl get all -n t3cicdbackend-datastore
```

7. View Data in GUI (PGAdmin)
```bash
minikube service pgadmin -n t3cicdbackend-datastore
```
It should automatically open a browser for you. If it does not open automatically, just use the URL for the tunnel for service pgadmin and append a path "/browser" which should look something like:
```
http://127.0.0.1:51925/browser
```
Login using the PGadmin username and password:
- Add a new server
    - In the General Tab, provide a name
    - In the Connection Tab, provide the:
        - host address: 192.168.49.2
        - port: 30432
        - Maintenance database: mydb
        - Username: root
    - Save it

In the Object Explorer on the left side of the window, you can access your newly added server. 
Go to the database "mydb" and right-click "Query Tool", here you can run SQL queries like create, insert, and select queries.

## Deploy Argo
1. Create namespace
```bash
kubectl create namespace t3cicdbackend
```

2. Deploy Argo service
```bash
kubectl apply -f https://github.com/argoproj/argo-workflows/releases/download/v3.6.0/quick-start-minimal.yaml -n argo
```

## Deploy Backend Cluster (t3cicdbackend)
### Deploy ConfigService and PipelineService
1. Create namespace
```bash
kubectl create namespace t3cicdbackend
```

2. Pull image
```bash
docker pull wp161/t3cicdbackend-configservice:latest
docker pull wp161/t3cicdbackend-pipelineservice:latest
docker tag wp161/t3cicdbackend-configservice:latest t3cicdbackend-configservice:latest
docker tag wp161/t3cicdbackend-pipelineservice:latest t3cicdbackend-pipelineservice:latest
```

3. Load image
```bash
minikube image load t3cicdbackend-configservice:latest
minikube image load t3cicdbackend-pipelineservice:latest
```

4. Deploy to K8s cluster
```bash
kubectl apply -f postgres-secret.yaml -n t3cicdbackend
kubectl apply -f configservice-deployment.yaml -n t3cicdbackend
kubectl apply -f pipelineservice-deployment.yaml -n t3cicdbackend
kubectl apply -f clusterrolebinding.yaml -n t3cicdbackend
```

5. Verify
```bash
kubectl get pods -n t3cicdbackend # copy the pod name
kubectl logs <pod-name> -n t3cicdbackend
```
Should see server started with log:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.4)

2024-11-19T05:12:28.998Z  INFO 1 --- [T3-CICD-Backend-ConfigService] [           main] n.c.configservice.ConfigApplication      : Starting ConfigApplication v0.0.1-SNAPSHOT using Java 21.0.5 with PID 1 (/app/app.jar started by root in /app)[T3-CICD-Backend-ConfigService] [           main] j.LocalContainerEntityManagerFactoryBean : Initialized JPA EntityManagerFactory for persistence unit 'default'
// other logs
2024-11-19T05:12:33.398Z  INFO 1 --- [T3-CICD-Backend-ConfigService] [           main] n.c.configservice.ConfigApplication      : Started ConfigApplication in 4.861 seconds (process running for 5.299)
```

6. [Optional] Test API connectivity directly (two ways, pick one)
    1. Port forwarding:
   ```bash
   kubectl get pods -n t3cicdbackend # copy the pod name
   kubectl port-forward <pod-name> 8080:8080 -n t3cicdbackend
   ```
   This command opens up a connection between your local machine's port `8080` and port `8080` on the specified pod.
   The pod listens on port `8080` for incoming traffic from your local machine.
   You can now access the application or service running inside the pod (on port `8080`) by making requests to `localhost:8080`:
    ```bash
    curl http://localhost:8080/actuator/health
   ```
   Expected response:
    ```bash
    {"status":"UP","groups":["liveness","readiness"]}%
   ```
    2. Minikube tunnel:
    ```bash
    minikube service configservice -n t3cicdbackend --url
   ```
   Should return something like (IP address may vary):
    ```
    http://127.0.0.1:52766 
    ❗  Because you are using a Docker driver on darwin, the terminal needs to be open to run it.
   ```
   Minikube creates a proxy that forwards traffic from your local machine to the Kubernetes service. The command prints a URL, which includes
   the IP address of the Minikube cluster on your local network, and the NodePort that the `configservice` is exposed on. You can then use this URL to access the service running in the Minikube cluster:
   ```bash
    curl http://127.0.0.1:52766/actuator/health
   ```
   Expected response:
    ```bash
    {"status":"UP","groups":["liveness","readiness"]}%
   ```

### Deploy Ingress Controller Locally
1. Add NGINX Ingress Controller to Minikube
```bash
minikube addons enable ingress
```
2. Verify
```bash
kubectl get pods -n ingress-nginx
```
Should return similar output:
```
NAME                                        READY   STATUS      RESTARTS    AGE
ingress-nginx-admission-create-g9g49        0/1     Completed   0          11m
ingress-nginx-admission-patch-rqp78         0/1     Completed   1          11m
ingress-nginx-controller-59b45fb494-26npt   1/1     Running     0
```
3. Deploy to K8s cluster
```bash
kubectl apply -f ingress-controller-local.yaml -n t3cicdbackend
```
4. Verity
```bash
kubectl get ingress -n t3cicdbackend
```
Should return similar output:
```bash
NAME                 CLASS    HOSTS       ADDRESS        PORTS   AGE
ingress-controller   <none>   ci-cd.com   192.168.49.2   80      53m
```
5. Test API connectivity
```bash
minikube tunnel
```
The output is similar to:
```
✅  Tunnel successfully started

📌  NOTE: Please do not close this terminal as this process must stay alive for the tunnel to be accessible ...

❗  The service/ingress ingress-controller requires privileged ports to be exposed: [80 443]
🔑  sudo permission will be asked for it.
🏃  Starting tunnel for service ingress-controller.
```
`sudo` permission is required for it, so provide the host password when prompted.

```bash
curl -i http://localhost/actuator/health
```
Expected response:
```
HTTP/1.1 200
Date: Sat, 23 Nov 2024 06:48:21 GMT
Content-Type: application/vnd.spring-boot.actuator.v3+json
Transfer-Encoding: chunked
Connection: keep-alive

{"status":"UP","groups":["liveness","readiness"]}%
```
> Note: For local deployment and testing, the host name is configured as `localhost` to simplify DNS resolution. 
> This allows the Ingress Controller to automatically map requests to `127.0.0.1`, enabling direct access without requiring additional DNS setup or overriding `/etc/hosts` configuration.
> 
> When the service is ready to be deployed to cloud, use your DNS provider (e.g., AWS Route 53, Google Cloud DNS, Azure DNS, GoDaddy) to configure the DNS record to point the host name to the Ingress Controller's IP.
> Use `ingress-controller-prod.yaml` with updated host name to deploy the Ingress Controller.