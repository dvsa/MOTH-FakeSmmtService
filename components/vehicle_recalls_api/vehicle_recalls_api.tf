module "vehicle_recalls_api" {
  source                    = "/var/lib/jenkins/workspace/Recalls/recalls-build/recalls-terraform/lambda"
  aws_region                = "${var.aws_region}"                             # has default value
  project                   = "${var.project}"                                # has default value
  environment               = "${var.environment}"
  lambda_s3_key             = "${var.lambda_s3_key}"
  lambda_s3_bucket          = "${var.lambda_s3_bucket}"
  lambda_function_name      = "vehicle-recalls-api"
  lambda_handler            = "src/main.handler"
  lambda_publish            = "true"
  lambda_memory_size        = "256"
  lambda_timeout            = "15"
  lambda_ver                = "$LATEST"
}

output "api_gateway_url" {
  value = "${module.vehicle_recalls_api.api_gateway_url}"
}