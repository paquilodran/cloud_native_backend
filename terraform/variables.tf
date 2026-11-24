variable "aws_region" {
  description = "Region de AWS para desplegar la infraestructura"
  type        = string
  default     = "us-east-1"
}

variable "aws_access_key" {
  description = "AWS Access Key ID"
  type        = string
  default     = ""
}

variable "aws_secret_key" {
  description = "AWS Secret Access Key"
  type        = string
  default     = ""
  sensitive   = true
}

variable "aws_session_token" {
  description = "AWS Session Token"
  type        = string
  default     = ""
  sensitive   = true
}

variable "aws_account_id" {
  description = "ID de la cuenta de AWS (Learner Lab)"
  type        = string
  default     = "088268627428"
}

variable "instance_type" {
  description = "Tipo de instancia EC2 (t3.large recomendado para cluster Kafka 3 brokers + RabbitMQ + DB)"
  type        = string
  default     = "t3.large"
}

variable "key_name" {
  description = "Nombre del Key Pair en AWS (en Learner Lab suele ser 'vockey')"
  type        = string
  default     = "vockey"
}

variable "create_key_pair" {
  description = "Crear nuevo aws_key_pair si no se esta usando el vockey existente de Learner Lab"
  type        = bool
  default     = false
}

variable "public_key_material" {
  description = "Material de clave publica SSH derivada de labsuser.pem"
  type        = string
  default     = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDSNFo2Nw91PDmFdNjK9Itt3XZkB8MAW2SgUmT/Bi835GROvQ3rXB0Apq0XJGF96uVkj4KFlb1EFHEmR3NF/h/qtSPTnMMzIb7wibgBWOwITQz23cUHLjW9y+09EsIs2keOV5DuIJtoxLWWHm9HsNfQUfcOd8lkpwZi3gG9EGH9Eo4GN7HQ4fDC6pU5BjhjNdomMD6auWZ2nQUgcDazSnnYOFRY3KPi729LntbSTgr+LRGLBb2AmlgJU6rz+kGf0s8fZUEFHV0BCFidlqXJIbr03NUGR19EKMJzeyXiZ/jTm9qLSgjZ7dc0gVEHQgzYBVdx//sksocttz8Oj1gu7T2T"
}

variable "use_lab_instance_profile" {
  description = "Vincular el perfil IAM existente LabInstanceProfile de AWS Academy"
  type        = bool
  default     = true
}

variable "azure_tenant_id" {
  description = "Tenant ID de Microsoft Entra ID"
  type        = string
  default     = "4531cbe0-83c7-406d-a972-e6302b1fb7d1"
}

variable "azure_client_id" {
  description = "Client ID de la API en Microsoft Entra ID"
  type        = string
  default     = "3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
}

variable "azure_app_id_uri" {
  description = "App ID URI de la API en Entra ID"
  type        = string
  default     = "api://3609dffc-ca49-4133-a6e5-2dbf3ba2a120"
}

variable "assign_eip" {
  description = "Asignar Elastic IP a la instancia EC2"
  type        = bool
  default     = false
}
