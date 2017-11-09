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

/*
  Sample map with quota and rate limit:
  {
    "quota_monthly" = 2500000
    "burst_limit" = 40
    "rate_limit" = 2
  }
*/
variable "api_rate_limit_vars" {
  type    = "map"
  default = {}
  description = "Pass rate limit and quota vars if required. Pass empty map if rate limiting not required"
}