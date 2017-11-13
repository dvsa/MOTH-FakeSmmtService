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
  lambda_env_vars           = "${var.vehicle_recalls_api_lambda_env_vars}"
  lambda_env_vars_dynamic   = {
    "SMMT_API_URI" = "${module.fake_smmt.api_gateway_url}/vincheck"
  }
  api_rate_limit_vars       = "${var.vehicle_recalls_api_rate_limit_vars}"
}

variable "vehicle_recalls_api_lambda_s3_key" {
  type    = "string"
  default = "default, when I am only creating bucket, I don't need it"
}

variable "vehicle_recalls_api_rate_limit_vars" {
  type    = "map"
  default = {
    "quota_monthly" = 2500000
    "burst_limit" = 40
    "rate_limit" = 2
  }
}

variable "vehicle_recalls_api_lambda_env_vars" {
  type    = "map"
  default = {
    "SMMT_API_URI" = "https://o2jf3z94li.execute-api.eu-west-2.amazonaws.com/dev/vincheck"
    "SMMT_API_KEY" = "localApiKey"
  }
}

output "vehicle_recalls_api_gateway_url" {
  value = "${module.vehicle_recalls_api.api_gateway_url}"
}

output "vehicle_recalls_api_key" {
  value = "${module.vehicle_recalls_api.api_key}"
}