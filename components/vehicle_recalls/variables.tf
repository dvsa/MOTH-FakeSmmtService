variable "aws_region" {
  type    = "string"
  default = "eu-west-1"
}

variable "project" {
  type    = "string"
  default = "vehicle_recalls"
}

variable "bucket_prefix" {
  type  = "string"
}

variable "environment" {
  type = "string"
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

variable "vehicle_recalls_api_lambda_s3_key" {
  type    = "string"
  default = "default, when I am only creating bucket, I don't need it"
}

variable "vehicle_recalls_api_service_name" {
  type    = "string"
  default = "Service name"
}

variable "vehicle_recalls_api_smmt_api_key" {
  type    = "string"
  default = "SMMT API key"
}

variable "vehicle_recalls_api_rate_limit_vars" {
  type    = "map"
  default = {
    "quota_monthly" = 2500000
    "burst_limit" = 40
    "rate_limit" = 2
  }
}

variable "vehicle_recalls_log_level" {
  type = "string"
}

variable "vehicle_recalls_api_timeout" {
  type    = "string"
  default = "15"
}