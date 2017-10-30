module "fake_smmt" {
  source                    = "../../recalls-terraform/lambda"
  aws_region                = "${var.aws_region}"                             # has default value
  project                   = "${var.project}"                                # has default value
  bucket_prefix             = "${var.bucket_prefix}"
  environment               = "${var.environment}"
  lambda_s3_key             = "${var.lambda_s3_key}"
  lambda_function_name      = "fake-smmt"
  lambda_handler            = "src/smmtService.handler"
  lambda_publish            = "true"
  lambda_memory_size        = "256"
  lambda_timeout            = "15"
  lambda_ver                = "$LATEST"
}

output "api_gateway_url" {
  value = "${module.fake_smmt.api_gateway_url}"
}