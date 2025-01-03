# Intergration tests

## Prerequisites

### Prepare the CLI

#### Install the CLI

Please follow `Installation Steps` in [README.md](../README.md) of this repo to install the CLI.

#### Verify the CLI installation

```bash
cicd --help
```

Should print the help messages of the CLI.

### Prepare the backend

#### Deploy backend in Kubernetes

Please follow the `README.md` in the [backend repo](https://github.com/CS6510-SEA-F24/t3-cicd-backend) to deploy the backend.
> Make sure you run `minikube tunnel` after the deployment to expose the cluster to incoming traffic. Otherwise, the CLI cannot reach out to the backend.

#### Verify the backend is deployed and can be accessed

```bash
curl -i http://localhost/actuator/health
```

Should return:

```bash
HTTP/1.1 200
Date: Sat, 23 Nov 2024 06:48:21 GMT
Content-Type: application/vnd.spring-boot.actuator.v3+json
Transfer-Encoding: chunked
Connection: keep-alive

{"status":"UP","groups":["liveness","readiness"]}%
```

> The date and time will be the actual time when you run the command.

## Run Test Scripts

Intergration test is written in shell scripts. Execute below scripts to run intergration tests.

### Validate a config file

Run below command from the project root:

```bash
chmod +x intergration_tests/validate.sh
./intergration_tests/validate.sh
```

`validate.sh` will run the `cicd validate` command to validate a pre-set Config File, and check if the Pipeline object is correctly parsed and saved in the DB.

### Run Pipeline

Run below command from the project root:

```bash
chmod +x intergration_tests/run.sh
./intergration_tests/run.sh
```

`run.sh` will run the `cicd validate` command to run the pipeline with a pre-set Config File, and check if all jobs in the pipeline are executed by verifying if all Argo logs for this pipeline have `COMPELTED` status in the DB.
