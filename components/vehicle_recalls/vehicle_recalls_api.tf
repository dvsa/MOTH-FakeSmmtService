module "vehicle_recalls_api" {
  source                    = "../../modules/lambda"
  aws_region                = "${var.aws_region}"                             # has default value
  project                   = "${var.project}"                                # has default value
  bucket_prefix             = "${var.bucket_prefix}"                          # has default value
  environment               = "${var.environment}"
  lambda_s3_key             = "${var.vehicle_recalls_api_lambda_s3_key}"
  lambda_function_name      = "vehicle-recalls-api"
  lambda_handler            = "src/main.handler"
  lambda_publish            = "true"
  lambda_memory_size        = "256"
  lambda_timeout            = "15"
  lambda_ver                = "$LATEST"
  lambda_env_vars           = {
    SMMT_API_URI = "${module.fake_smmt.api_gateway_url}/vincheck"
    SMMT_API_KEY = "${var.vehicle_recalls_api_smmt_api_key}",
    SERVICE_NAME = "${var.vehicle_recalls_api_service_name}",
    SERVICE_ENV = "${var.environment}",
    RECALL_LOG_LEVEL = "${var.vehicle_recalls_log_level}",
  }
  api_rate_limit_vars       = "${var.vehicle_recalls_api_rate_limit_vars}"
}