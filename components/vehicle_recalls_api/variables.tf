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

variable "lambda_s3_key" {
  type    = "string"
  default = "default, when I am only creating bucket, I don't need it"
}

variable "lambda_env_vars" {
  type    = "map"
  default = {
    "lambda_smmt_uri" = "https://o2jf3z94li.execute-api.eu-west-2.amazonaws.com/dev/vincheck"
    "lambda_smmt_key" = "localApiKey"
  }
}