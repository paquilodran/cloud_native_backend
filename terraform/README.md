# Infraestructura como Código (IaC) - Pedidos360 en AWS con Terraform

Este módulo de Terraform automatiza el aprovisionamiento completo de la infraestructura en **Amazon Web Services (AWS)** para el sistema **Pedidos360**, cumpliendo con los estándares de **EP5 (Encargo)** y **EP6 (Presentación)** de la asignatura *DSY1107 - Desarrollo Cloud Native I*.

Diseñado específicamente para ser 100% compatible con **AWS Academy Learner Lab** (cuenta `088268627428`, región `us-east-1`, rol `LabRole` y perfil `LabInstanceProfile`).

---

## 1. Arquitectura desplegada

1. **Amazon EC2 (`t3.large`, Ubuntu 22.04 LTS, 30 GB gp3)**:
   - Contenedores Docker orquestados vía `docker-compose`:
     - **PostgreSQL 16**: Base de datos relacional para pedidos.
     - **Clúster RabbitMQ (2 nodos)**: Mensajería asíncrona punto a punto y publish/subscribe con colas y DLQs.
     - **Ensemble Zookeeper (3 nodos)**: Quorum y consenso para Apache Kafka.
     - **Clúster Apache Kafka (3 brokers)**: Streaming analítico y auditoría distribuida (replicación 3, min ISR 2).
     - **Kafka-UI (`:8085`)**: Consola de gestión y supervisión en tiempo real de brokers, tópicos, particiones y lag.
     - **Init-Kafka**: Contenedor de inicialización que asegura la creación de los tópicos requeridos (`pedidos.analytics.stream` y `pedidos.audit.stream`).
     - **Backend Spring Boot 3 (`:8080`)**: Microservicio REST, Resource Server con validación JWT, productores y consumidores desacoplados.
     - **Frontend Angular 19 (`:80`)**: Aplicación web SPA servida con Nginx, integración con Microsoft Entra ID (MSAL).

2. **Security Group (`pedidos360-security-group`)**:
   - Puertos habilitados: `22` (SSH), `80` (HTTP), `443` (HTTPS), `8080` (Spring Boot API), `8085` (Kafka-UI), `15672/15673` (RabbitMQ UI), `5672/5673` (AMQP), `9092-9094` (Kafka), `2181-2183` (Zookeeper), `5432` (PostgreSQL).

3. **Amazon API Gateway v2 (HTTP API - `Pedidos360-api`)**:
   - **Autorizador JWT Microsoft**:
     - Emisor: `https://login.microsoftonline.com/4531cbe0-83c7-406d-a972-e6302b1fb7d1/v2.0`
     - Audiencia: `api://3609dffc-ca49-4133-a6e5-2dbf3ba2a120` y `3609dffc-ca49-4133-a6e5-2dbf3ba2a120`
   - **Rutas configuradas**:
     - `ANY /{proxy+}`: Protegida por JWT (retorna 401 sin token, 200 con token válido).
     - `ANY /api/public/{proxy+}`: Pública sin autorizador (para `/health`, endpoints de demostración y APIs administrativas).
     - `OPTIONS /{proxy+}`: Preflight CORS.
   - **Integración HTTP Proxy**: Redirige hacia el backend Spring Boot en la instancia EC2 (`http://<EC2_IP>:8080/{proxy}`).
   - **Stage `$default`**: Con auto-deploy y logs detallados en **AWS CloudWatch**.

---

## 2. Instrucciones de Despliegue Rápido

### Prerrequisitos
- Tener instalado [Terraform](https://developer.hashicorp.com/terraform/downloads) (>= 1.3).
- Obtener credenciales de sesión activas de **AWS Academy Learner Lab** (desde la consola del laboratorio -> *AWS Details* -> *AWS CLI*).

### Paso 1: Configurar credenciales de AWS
Copia y pega en tu terminal las credenciales temporales de AWS Learner Lab:

```bash
export AWS_ACCESS_KEY_ID="ASI..."
export AWS_SECRET_ACCESS_KEY="..."
export AWS_SESSION_TOKEN="..."
export AWS_DEFAULT_REGION="us-east-1"
```

### Paso 2: Preparar variables de Terraform
Copia el archivo de ejemplo:

```bash
cp terraform.tfvars.example terraform.tfvars
```

### Paso 3: Inicializar y desplegar
```bash
terraform init
terraform plan
terraform apply -auto-approve
```

### Paso 4: Obtener URLs y conectar
Al terminar `terraform apply`, la salida mostrará las URLs públicas del sistema:
- **Frontend Angular**: `http://<EC2_IP>`
- **API Gateway Invoke URL**: `https://<API_ID>.execute-api.us-east-1.amazonaws.com`
- **Kafka UI**: `http://<EC2_IP>:8085`
- **RabbitMQ Management**: `http://<EC2_IP>:15672`
- **Backend Health Check**: `http://<EC2_IP>:8080/api/public/health`

Para conectar vía SSH a la instancia:
```bash
chmod 400 ../labsuser.pem
ssh -i ../labsuser.pem ubuntu@<EC2_IP>
```

Para verificar el log del despliegue automático en la máquina:
```bash
tail -f /var/log/pedidos360-init.log
```
O ver el estado de los contenedores Docker:
```bash
docker ps
```
