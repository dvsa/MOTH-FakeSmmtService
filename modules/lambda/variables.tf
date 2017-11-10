variable "aws_region" {
  type    = "string"
  default = "eu-west-1"
}

variable "project" {
  type    = "string"
  default = "vehicle_recalls"
}

variable "bucket_prefix" {
  type = "string"
}

variable "environment" {
  type = "string"
}

variable "lambda_env_vars" {
  type    = "map"
  default = {}
}

variable "lambda_publish" {
  type        = "string"
  description = "Lambda publish switch"
}

variable "lambda_memory_size" {
  type        = "string"
  description = "Amount of memory in MB Lambda Function can use at runtime"
}

variable "lambda_timeout" {
  type        = "string"
  description = "The amount of time Lambda Function has to run in seconds"
}

variable "lambda_ver" {
  type        = "string"
  description = "Lambda function version"
}

variable "lambda_function_name" {
  type        = "string"
  description = "Function name"
}

variable "lambda_handler" {
  type        = "string"
  description = "Lambda Handler name"
}

variable "lambda_s3_key" {
  type        = "string"
  description = "Lambda S3 Key"
}

variable "api_rate_limit_vars" {
  type    = "map"
  default = {}
  description = "Pass rate limit and quota vars if required. Pass empty map if rate limiting not required"
}
