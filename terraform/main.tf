terraform {
  required_version = ">= 1.3.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region     = var.aws_region
  access_key = var.aws_access_key
  secret_key = var.aws_secret_key
  token      = var.aws_session_token

  default_tags {
    tags = {
      Project     = "Pedidos360"
      Environment = "Production"
      Course      = "DSY1107-Desarrollo-Cloud-Native-I"
      Students    = "Patricio-Quilodran-Sebastian-Dinamarca"
      ManagedBy   = "Terraform"
    }
  }
}

# =============================================================================
# DATA SOURCES: VPC Y SUBNET POR DEFECTO
# =============================================================================

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# AMI Oficial Ubuntu 22.04 LTS x86_64
data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# =============================================================================
# KEY PAIR (OPCIONAL O REUTILIZACION DE VOCKEY DE LEARNER LAB)
# =============================================================================

resource "aws_key_pair" "custom_key" {
  count      = var.create_key_pair ? 1 : 0
  key_name   = "pedidos360-key"
  public_key = var.public_key_material
}

locals {
  selected_key_name = var.create_key_pair ? aws_key_pair.custom_key[0].key_name : var.key_name
  instance_profile  = var.use_lab_instance_profile ? "LabInstanceProfile" : null
}

# =============================================================================
# SECURITY GROUP: PUERTOS PARA EL STACK DISTRIBUIDO
# =============================================================================

resource "aws_security_group" "pedidos360_sg" {
  name        = "pedidos360-security-group"
  description = "Reglas de entrada y salida para cluster Pedidos360 (Spring Boot, Kafka, RabbitMQ, UI)"
  vpc_id      = data.aws_vpc.default.id

  # SSH
  ingress {
    description = "SSH Administration"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Frontend HTTP / Nginx
  ingress {
    description = "Frontend Angular Nginx HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Frontend HTTPS
  ingress {
    description = "Frontend HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Spring Boot Backend API
  ingress {
    description = "Spring Boot REST API (Resource Server)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Kafka UI
  ingress {
    description = "Kafka-UI Web Management (Topicos, Brokers, Lag)"
    from_port   = 8085
    to_port     = 8085
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # RabbitMQ Management UI Nodo 1
  ingress {
    description = "RabbitMQ Management UI Nodo 1"
    from_port   = 15672
    to_port     = 15672
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # RabbitMQ Management UI Nodo 2
  ingress {
    description = "RabbitMQ Management UI Nodo 2"
    from_port   = 15673
    to_port     = 15673
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # RabbitMQ AMQP Ports
  ingress {
    description = "RabbitMQ AMQP Protocol"
    from_port   = 5672
    to_port     = 5673
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Kafka External Listeners (Brokers 1, 2, 3)
  ingress {
    description = "Kafka Cluster Brokers (9092, 9093, 9094)"
    from_port   = 9092
    to_port     = 9094
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Zookeeper Quorum Client Ports (2181, 2182, 2183)
  ingress {
    description = "Zookeeper Quorum Ensembles"
    from_port   = 2181
    to_port     = 2183
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # PostgreSQL
  ingress {
    description = "PostgreSQL Database"
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Todo el trafico saliente
  egress {
    description = "All outbound traffic"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "pedidos360-security-group"
  }
}

# =============================================================================
# INSTANCIA EC2: CLOUD NATIVE ENGINE (DOCKER COMPOSE MULTI-CONTAINER)
# =============================================================================

resource "aws_instance" "pedidos360" {
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  key_name                    = local.selected_key_name
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.pedidos360_sg.id]
  iam_instance_profile        = local.instance_profile
  associate_public_ip_address = true

  root_block_device {
    volume_size           = 30
    volume_type           = "gp3"
    delete_on_termination = true
    tags = {
      Name = "pedidos360-root-volume"
    }
  }

  user_data = <<-EOF
              #!/bin/bash
              set -e
              exec > >(tee /var/log/pedidos360-init.log|logger -t user-data -s 2>/dev/console) 2>&1

              echo "=== INICIANDO CONFIGURACION PEDIDOS360 CLOUD NATIVE ==="

              # 1. Actualizar sistema e instalar prerequisitos
              apt-get update -y
              apt-get install -y ca-certificates curl gnupg lsb-release git jq ufw

              # 2. Instalar Docker y Docker Compose plugin oficial
              mkdir -m 0755 -p /etc/apt/keyrings
              curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
              echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
              apt-get update -y
              apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

              usermod -aG docker ubuntu
              systemctl enable docker
              systemctl start docker

              # Ajustar memoria virtual para Apache Kafka y contenedores JVM
              sysctl -w vm.max_map_count=262144
              echo "vm.max_map_count=262144" >> /etc/sysctl.conf

              # 3. Clonar repositorios oficiales de Pedidos360
              APP_DIR="/home/ubuntu/pedidos360-deploy"
              mkdir -p "$APP_DIR"
              cd "$APP_DIR"

              git clone https://github.com/paquilodran/cloud_native_backend.git backend
              git clone https://github.com/paquilodran/cloud_native_frontend.git frontend

              # Crear archivo docker-compose maestro
              cat << 'DOCKERCOMPOSE' > "$APP_DIR/docker-compose.yml"
              version: '3.8'

              networks:
                pedidos360-net:
                  name: pedidos360-net
                  driver: bridge

              volumes:
                pedidos360_pg_data:
                rabbitmq_node1_data:
                rabbitmq_node2_data:
                zk1_data:
                zk1_log:
                zk2_data:
                zk2_log:
                zk3_data:
                zk3_log:
                kafka1_data:
                kafka2_data:
                kafka3_data:

              services:
                postgres:
                  image: postgres:16-alpine
                  container_name: pedidos360-db
                  networks:
                    - pedidos360-net
                  ports:
                    - "5432:5432"
                  environment:
                    POSTGRES_DB: pedidos360_db
                    POSTGRES_USER: pedidos
                    POSTGRES_PASSWORD: pedidos
                  volumes:
                    - pedidos360_pg_data:/var/lib/postgresql/data
                  healthcheck:
                    test: ["CMD-SHELL", "pg_isready -U pedidos -d pedidos360_db"]
                    interval: 5s
                    timeout: 5s
                    retries: 5

                rabbitmq-node1:
                  image: rabbitmq:4-management
                  container_name: rabbitmq-cluster-node1
                  hostname: rabbit1
                  networks:
                    - pedidos360-net
                  ports:
                    - "5672:5672"
                    - "15672:15672"
                  environment:
                    RABBITMQ_ERLANG_COOKIE: "SECRET_COOKIE_DUOC_CLOUD_NATIVE"
                    RABBITMQ_DEFAULT_USER: guest
                    RABBITMQ_DEFAULT_PASS: guest
                    RABBITMQ_NODENAME: rabbit@rabbit1
                  volumes:
                    - rabbitmq_node1_data:/var/lib/rabbitmq
                  healthcheck:
                    test: ["CMD", "rabbitmq-diagnostics", "ping"]
                    interval: 10s
                    timeout: 5s
                    retries: 5

                rabbitmq-node2:
                  image: rabbitmq:4-management
                  container_name: rabbitmq-cluster-node2
                  hostname: rabbit2
                  networks:
                    - pedidos360-net
                  ports:
                    - "5673:5672"
                    - "15673:15672"
                  environment:
                    RABBITMQ_ERLANG_COOKIE: "SECRET_COOKIE_DUOC_CLOUD_NATIVE"
                    RABBITMQ_DEFAULT_USER: guest
                    RABBITMQ_DEFAULT_PASS: guest
                    RABBITMQ_NODENAME: rabbit@rabbit2
                  volumes:
                    - rabbitmq_node2_data:/var/lib/rabbitmq
                  depends_on:
                    - rabbitmq-node1
                  healthcheck:
                    test: ["CMD", "rabbitmq-diagnostics", "ping"]
                    interval: 10s
                    timeout: 5s
                    retries: 5

                zookeeper-1:
                  image: confluentinc/cp-zookeeper:7.5.3
                  container_name: pedidos360-zookeeper-1
                  hostname: zookeeper-1
                  networks:
                    - pedidos360-net
                  ports:
                    - "2181:2181"
                  environment:
                    ZOOKEEPER_SERVER_ID: 1
                    ZOOKEEPER_CLIENT_PORT: 2181
                    ZOOKEEPER_TICK_TIME: 2000
                    ZOOKEEPER_INIT_LIMIT: 5
                    ZOOKEEPER_SYNC_LIMIT: 2
                    ZOOKEEPER_SERVERS: "zookeeper-1:2888:3888;zookeeper-2:2888:3888;zookeeper-3:2888:3888"
                  volumes:
                    - zk1_data:/var/lib/zookeeper/data
                    - zk1_log:/var/lib/zookeeper/log

                zookeeper-2:
                  image: confluentinc/cp-zookeeper:7.5.3
                  container_name: pedidos360-zookeeper-2
                  hostname: zookeeper-2
                  networks:
                    - pedidos360-net
                  ports:
                    - "2182:2182"
                  environment:
                    ZOOKEEPER_SERVER_ID: 2
                    ZOOKEEPER_CLIENT_PORT: 2182
                    ZOOKEEPER_TICK_TIME: 2000
                    ZOOKEEPER_INIT_LIMIT: 5
                    ZOOKEEPER_SYNC_LIMIT: 2
                    ZOOKEEPER_SERVERS: "zookeeper-1:2888:3888;zookeeper-2:2888:3888;zookeeper-3:2888:3888"
                  volumes:
                    - zk2_data:/var/lib/zookeeper/data
                    - zk2_log:/var/lib/zookeeper/log

                zookeeper-3:
                  image: confluentinc/cp-zookeeper:7.5.3
                  container_name: pedidos360-zookeeper-3
                  hostname: zookeeper-3
                  networks:
                    - pedidos360-net
                  ports:
                    - "2183:2183"
                  environment:
                    ZOOKEEPER_SERVER_ID: 3
                    ZOOKEEPER_CLIENT_PORT: 2183
                    ZOOKEEPER_TICK_TIME: 2000
                    ZOOKEEPER_INIT_LIMIT: 5
                    ZOOKEEPER_SYNC_LIMIT: 2
                    ZOOKEEPER_SERVERS: "zookeeper-1:2888:3888;zookeeper-2:2888:3888;zookeeper-3:2888:3888"
                  volumes:
                    - zk3_data:/var/lib/zookeeper/data
                    - zk3_log:/var/lib/zookeeper/log

                kafka-1:
                  image: confluentinc/cp-kafka:7.5.3
                  container_name: pedidos360-kafka-1
                  hostname: kafka-1
                  networks:
                    - pedidos360-net
                  ports:
                    - "9092:9092"
                  depends_on:
                    - zookeeper-1
                    - zookeeper-2
                    - zookeeper-3
                  environment:
                    KAFKA_BROKER_ID: 1
                    KAFKA_ZOOKEEPER_CONNECT: "zookeeper-1:2181,zookeeper-2:2182,zookeeper-3:2183"
                    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
                    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-1:29092,PLAINTEXT_HOST://localhost:9092
                    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
                    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
                    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
                    KAFKA_DEFAULT_REPLICATION_FACTOR: 3
                    KAFKA_NUM_PARTITIONS: 3
                    KAFKA_MIN_INSYNC_REPLICAS: 2
                    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
                  volumes:
                    - kafka1_data:/var/lib/kafka/data

                kafka-2:
                  image: confluentinc/cp-kafka:7.5.3
                  container_name: pedidos360-kafka-2
                  hostname: kafka-2
                  networks:
                    - pedidos360-net
                  ports:
                    - "9093:9093"
                  depends_on:
                    - zookeeper-1
                    - zookeeper-2
                    - zookeeper-3
                  environment:
                    KAFKA_BROKER_ID: 2
                    KAFKA_ZOOKEEPER_CONNECT: "zookeeper-1:2181,zookeeper-2:2182,zookeeper-3:2183"
                    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
                    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-2:29093,PLAINTEXT_HOST://localhost:9093
                    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
                    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
                    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
                    KAFKA_DEFAULT_REPLICATION_FACTOR: 3
                    KAFKA_NUM_PARTITIONS: 3
                    KAFKA_MIN_INSYNC_REPLICAS: 2
                    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
                  volumes:
                    - kafka2_data:/var/lib/kafka/data

                kafka-3:
                  image: confluentinc/cp-kafka:7.5.3
                  container_name: pedidos360-kafka-3
                  hostname: kafka-3
                  networks:
                    - pedidos360-net
                  ports:
                    - "9094:9094"
                  depends_on:
                    - zookeeper-1
                    - zookeeper-2
                    - zookeeper-3
                  environment:
                    KAFKA_BROKER_ID: 3
                    KAFKA_ZOOKEEPER_CONNECT: "zookeeper-1:2181,zookeeper-2:2182,zookeeper-3:2183"
                    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
                    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka-3:29094,PLAINTEXT_HOST://localhost:9094
                    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
                    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
                    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
                    KAFKA_DEFAULT_REPLICATION_FACTOR: 3
                    KAFKA_NUM_PARTITIONS: 3
                    KAFKA_MIN_INSYNC_REPLICAS: 2
                    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
                  volumes:
                    - kafka3_data:/var/lib/kafka/data

                kafka-ui:
                  image: provectuslabs/kafka-ui:latest
                  container_name: pedidos360-kafka-ui
                  networks:
                    - pedidos360-net
                  ports:
                    - "8085:8080"
                  depends_on:
                    - kafka-1
                    - kafka-2
                    - kafka-3
                  environment:
                    KAFKA_CLUSTERS_0_NAME: pedidos360-cluster
                    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka-1:29092,kafka-2:29093,kafka-3:29094
                    KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper-1:2181,zookeeper-2:2182,zookeeper-3:2183
                    DYNAMIC_CONFIG_ENABLED: 'true'

                init-kafka:
                  image: confluentinc/cp-kafka:7.5.3
                  container_name: pedidos360-init-kafka
                  networks:
                    - pedidos360-net
                  depends_on:
                    - kafka-1
                    - kafka-2
                    - kafka-3
                  entrypoint: [ "/bin/sh", "-c" ]
                  command: >
                    "cub kafka-ready -b kafka-1:29092 1 40 &&
                     cub kafka-ready -b kafka-2:29093 1 40 &&
                     cub kafka-ready -b kafka-3:29094 1 40 &&
                     kafka-topics --bootstrap-server kafka-1:29092 --create --if-not-exists --topic pedidos.analytics.stream --partitions 3 --replication-factor 3 --config retention.ms=86400000 --config cleanup.policy=delete --config min.insync.replicas=2 &&
                     kafka-topics --bootstrap-server kafka-1:29092 --create --if-not-exists --topic pedidos.audit.stream --partitions 3 --replication-factor 3 --config retention.ms=604800000 --config cleanup.policy=compact,delete --config min.insync.replicas=2 &&
                     echo '=== TOPICOS CREADOS EXITOSAMENTE ===' &&
                     kafka-topics --bootstrap-server kafka-1:29092 --describe"

                backend:
                  build:
                    context: ./backend
                    dockerfile: Dockerfile
                  container_name: pedidos360-backend
                  hostname: backend
                  networks:
                    - pedidos360-net
                  ports:
                    - "8080:8080"
                  depends_on:
                    postgres:
                      condition: service_healthy
                    rabbitmq-node1:
                      condition: service_healthy
                    rabbitmq-node2:
                      condition: service_healthy
                    kafka-1:
                      condition: service_started
                    kafka-2:
                      condition: service_started
                    kafka-3:
                      condition: service_started
                  environment:
                    SPRING_PROFILES_ACTIVE: postgres
                    DB_HOST: postgres
                    DB_PORT: 5432
                    DB_NAME: pedidos360_db
                    DB_USERNAME: pedidos
                    DB_PASSWORD: pedidos
                    RABBITMQ_HOST: rabbit1
                    RABBITMQ_PORT: 5672
                    RABBITMQ_USER: guest
                    RABBITMQ_PASSWORD: guest
                    KAFKA_BOOTSTRAP_SERVERS: "kafka-1:29092,kafka-2:29093,kafka-3:29094"
                    CORS_ALLOWED_ORIGINS: "*"
                    AZURE_TENANT_ID: "4531cbe0-83c7-406d-a972-e6302b1fb7d1"
                    AZURE_API_CLIENT_ID: "3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
                    AZURE_API_APP_ID_URI: "api://3609dffc-ca49-4133-a6e5-2dbf3ba2a120"

                frontend:
                  build:
                    context: ./frontend
                    dockerfile: Dockerfile
                  container_name: pedidos360-frontend
                  networks:
                    - pedidos360-net
                  ports:
                    - "80:80"
                  depends_on:
                    - backend
              DOCKERCOMPOSE

              chown -R ubuntu:ubuntu "$APP_DIR"

              # 4. Levantar todos los servicios en segundo plano
              docker compose -f "$APP_DIR/docker-compose.yml" up -d --build

              echo "=== DESPLIEGUE FINALIZADO EXITOSAMENTE ==="
              EOF

  tags = {
    Name = "Pedidos360"
  }
}

# =============================================================================
# ELASTIC IP (OPCIONAL)
# =============================================================================

resource "aws_eip" "pedidos360_eip" {
  count    = var.assign_eip ? 1 : 0
  instance = aws_instance.pedidos360.id
  domain   = "vpc"

  tags = {
    Name = "pedidos360-eip"
  }
}

locals {
  target_ip = var.assign_eip ? aws_eip.pedidos360_eip[0].public_ip : aws_instance.pedidos360.public_ip
}

# =============================================================================
# AWS API GATEWAY V2 (HTTP API CON MICROSOFT ENTRA ID JWT AUTHORIZER)
# =============================================================================

resource "aws_apigatewayv2_api" "http_api" {
  name          = "Pedidos360-api"
  protocol_type = "HTTP"
  description   = "API Manager Cloud Native para Pedidos360 con validacion JWT Microsoft Entra ID"

  cors_configuration {
    allow_credentials = false
    allow_headers     = ["Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"]
    allow_methods     = ["GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"]
    allow_origins     = ["http://localhost:4200", "http://localhost", "https://pedidos360eva2.duckdns.org", "*"]
    max_age           = 3600
  }

  tags = {
    Name = "Pedidos360-api"
  }
}

# Autorizador JWT contra Microsoft Entra ID
resource "aws_apigatewayv2_authorizer" "jwt_auth" {
  api_id           = aws_apigatewayv2_api.http_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  name             = "Microsoft"

  jwt_configuration {
    issuer   = "https://login.microsoftonline.com/${var.azure_tenant_id}/v2.0"
    audience = [var.azure_app_id_uri, var.azure_client_id]
  }
}

# Integración HTTP Proxy apuntando a la IP pública de la instancia EC2
resource "aws_apigatewayv2_integration" "backend_integration" {
  api_id                 = aws_apigatewayv2_api.http_api.id
  integration_type       = "HTTP_PROXY"
  integration_method     = "ANY"
  integration_uri        = "http://${local.target_ip}:8080/{proxy}"
  payload_format_version = "1.0"
  description            = "Proxy hacia el backend Spring Boot en EC2 :8080"
}

# Ruta protegida por JWT: ANY /{proxy+}
resource "aws_apigatewayv2_route" "protected_proxy_route" {
  api_id             = aws_apigatewayv2_api.http_api.id
  route_key          = "ANY /{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.backend_integration.id}"
  authorizer_id      = aws_apigatewayv2_authorizer.jwt_auth.id
  authorization_type = "JWT"
}

# Ruta pública sin autorizador: ANY /api/public/{proxy+} (Health check, RabbitMQ y Kafka demo/admin)
resource "aws_apigatewayv2_route" "public_proxy_route" {
  api_id             = aws_apigatewayv2_api.http_api.id
  route_key          = "ANY /api/public/{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.backend_integration.id}"
  authorization_type = "NONE"
}

# Ruta CORS preflight: OPTIONS /{proxy+}
resource "aws_apigatewayv2_route" "cors_options_route" {
  api_id             = aws_apigatewayv2_api.http_api.id
  route_key          = "OPTIONS /{proxy+}"
  target             = "integrations/${aws_apigatewayv2_integration.backend_integration.id}"
  authorization_type = "NONE"
}

# Etapa $default con Auto-Deploy
resource "aws_apigatewayv2_stage" "default_stage" {
  api_id      = aws_apigatewayv2_api.http_api.id
  name        = "$default"
  auto_deploy = true

  access_log_settings {
    destination_arn = aws_cloudwatch_log_group.api_gw_logs.arn
    format = jsonencode({
      requestId      = "$context.requestId"
      ip             = "$context.identity.sourceIp"
      requestTime    = "$context.requestTime"
      httpMethod     = "$context.httpMethod"
      routeKey       = "$context.routeKey"
      status         = "$context.status"
      protocol       = "$context.protocol"
      responseLength = "$context.responseLength"
      authorizerErr  = "$context.authorizer.error"
    })
  }

  tags = {
    Name = "default-stage"
  }
}

# CloudWatch Log Group para trazabilidad de peticiones del API Gateway
resource "aws_cloudwatch_log_group" "api_gw_logs" {
  name              = "/aws/apigateway/Pedidos360-api"
  retention_in_days = 7

  tags = {
    Name = "pedidos360-api-gw-logs"
  }
}
