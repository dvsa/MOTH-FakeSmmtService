module "fake_smmt" {
  source                    = "../../modules/lambda"
  aws_region                = "${var.aws_region}"                             # has default value
  project                   = "${var.project}"                                # has default value
  bucket_prefix             = "${var.bucket_prefix}"
  environment               = "${var.environment}"
  lambda_s3_key             = "${var.fake_smmt_lambda_s3_key}"
  lambda_function_name      = "fake-smmt"
  lambda_handler            = "src/smmtService.handler"
  lambda_publish            = "true"
  lambda_memory_size        = "256"
  lambda_timeout            = "15"
  lambda_ver                = "$LATEST"
  lambda_env_vars           = "${var.fake_smmt_lambda_env_vars}"
}

variable "fake_smmt_lambda_s3_key" {
  type    = "string"
  default = "default, when I am only creating bucket, I don't need it"
}

variable "fake_smmt_lambda_env_vars" {
  type    = "map"
  default = {
    "default" = "default"
  }
}

output "fake_smmt_api_gateway_url" {
  value = "${module.fake_smmt.api_gateway_url}"
}