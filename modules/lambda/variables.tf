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