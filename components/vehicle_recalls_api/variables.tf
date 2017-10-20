variable "aws_region" {
  type    = "string"
  default = "eu-west-1"
}

variable "project" {
  type    = "string"
  default = "vehicle_recalls"
}

variable "environment" {
  type = "string"
}

variable "bucket_prefix" {
  type    = "string"
  default = "uk.gov.dvsa.vehicle-recalls."
}

variable "bucket_versioning_enabled" {
  type    = "string"
  default = false
}

variable "lambda_s3_key" {
  type    = "string"
  default = "default, when I am only creating bucket, I don't need it"
}