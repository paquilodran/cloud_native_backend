output "ec2_public_ip" {
  description = "Direccion IPv4 publica de la instancia EC2"
  value       = local.target_ip
}

output "ssh_command" {
  description = "Comando para conectar por SSH usando la clave labsuser.pem"
  value       = "ssh -i labsuser.pem ubuntu@${local.target_ip}"
}

output "api_gateway_id" {
  description = "Identificador de la HTTP API en API Gateway"
  value       = aws_apigatewayv2_api.http_api.id
}

output "api_gateway_invoke_url" {
  description = "URL base de invocacion de API Gateway ($default stage)"
  value       = aws_apigatewayv2_stage.default_stage.invoke_url
}

output "frontend_url" {
  description = "URL de acceso al Frontend Angular (Nginx)"
  value       = "http://${local.target_ip}"
}

output "backend_health_url" {
  description = "URL de comprobacion de salud del Backend"
  value       = "http://${local.target_ip}:8080/api/public/health"
}

output "kafka_ui_url" {
  description = "URL de acceso a Kafka-UI para administracion del cluster de streaming"
  value       = "http://${local.target_ip}:8085"
}

output "rabbitmq_management_node1_url" {
  description = "URL de la consola de administracion de RabbitMQ Nodo 1"
  value       = "http://${local.target_ip}:15672"
}

output "rabbitmq_management_node2_url" {
  description = "URL de la consola de administracion de RabbitMQ Nodo 2"
  value       = "http://${local.target_ip}:15673"
}

output "jwt_authorizer_id" {
  description = "Identificador del autorizador JWT Microsoft Entra ID en API Gateway"
  value       = aws_apigatewayv2_authorizer.jwt_auth.id
}
