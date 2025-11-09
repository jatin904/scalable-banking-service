# Customer Service Microservice

This project is part of the Scalable Services assignment for building a microservice-based system.  
The **Customer Service** microservice manages customer records and communicates with external services (mocked in this implementation).  
The project is fully containerized using Docker and deployed into a Kubernetes cluster (Minikube).

📌 GitHub Repository:  
https://github.com/Dimple-AA/customer-service-microservice

---

## 📌 Features

- Full Customer CRUD operations (Create, Read, Update, Delete)
- Bulk upload customer data using CSV
- Connects to mock Account and Notification services
- Stores customer data in MongoDB
- Includes `/health` endpoint for service availability monitoring
- Supports deployment via Docker & Kubernetes

---

## 🏗️ Project Structure

```
customer-service/
│── controllers/
│── models/
│── routes/
│── mock-services/         # Mock Account & Notification services
│── k8s/                   # Kubernetes deployment files
│── Dockerfile
│── docker-compose.yml
│── server.js
│── .env
```

---

## 🔧 Technologies Used

| Component        | Technology             |
| ---------------- | ---------------------- |
| Backend API      | Node.js + Express      |
| Database         | MongoDB                |
| Communication    | REST APIs              |
| Containerization | Docker, Docker Compose |
| Orchestration    | Kubernetes (Minikube)  |
| Language         | JavaScript             |

---

## 🚀 API Endpoints

| Method | Endpoint                 | Description                     |
| ------ | ------------------------ | ------------------------------- |
| GET    | `/customers`             | Retrieve all customers          |
| GET    | `/customers/:id`         | Get customer by ID              |
| POST   | `/customers`             | Create a new customer           |
| PUT    | `/customers/:id`         | Update existing customer        |
| DELETE | `/customers/:id`         | Delete a customer               |
| POST   | `/customers/bulk-upload` | Upload multiple records via CSV |
| GET    | `/health`                | Service health check            |

---

## 🛠️ Run Locally (Without Docker)

```bash
npm install
node server.js
```

Open in browser:

```
http://localhost:3000/customers
```

---

## 🐳 Run Using Docker Compose

```bash
docker-compose up --build
docker ps
```

---

## ☸️ Deploying on Kubernetes (Minikube)

```bash
minikube start
kubectl apply -f k8s/
kubectl get pods
kubectl get svc
minikube ip
minikube service customer-service
```

Access service:

```
http://<minikube-ip>:30080/customers
```

---

## 📊 Enabling Resource Monitoring (Metrics Server)

The Metrics Server allows viewing **CPU and Memory usage** of nodes and pods.

### 1️⃣ Apply Metrics Server

```bash
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 2️⃣ Patch Metrics Server for Minikube

Create patch file:

```bash
notepad metrics-patch.json
```

Paste and save:

```json
[
  {
    "op": "add",
    "path": "/spec/template/spec/containers/0/args/-",
    "value": "--kubelet-insecure-tls"
  },
  {
    "op": "add",
    "path": "/spec/template/spec/containers/0/args/-",
    "value": "--kubelet-preferred-address-types=InternalIP"
  }
]
```

Apply the patch:

```bash
kubectl -n kube-system patch deployment metrics-server --type=json --patch-file metrics-patch.json
```

### 3️⃣ Verify Metrics Server is Running

```bash
kubectl get pods -n kube-system | findstr metrics
```

Expected:

```
metrics-server-xxxxx   1/1   Running
```

### 4️⃣ View Resource Consumption

```bash
kubectl top nodes
kubectl top pods
```

Sample Output:

```
NAME        CPU(cores)   MEMORY(bytes)
minikube    120m         1450Mi
```

---

## ❤️ Health Check Endpoint

```bash
GET /health
```

Response:

```json
{ "status": "UP" }
```

---

## 📚 Learnings

- Built microservices with separation of concerns
- Understood containerization via Docker
- Deployed services on Kubernetes using Minikube
- Implemented service health checks and monitoring with Metrics Server

---

## 👤 Author

**Name:** Dimple A A  
**Course:** M.Tech — Scalable Services Assignment
