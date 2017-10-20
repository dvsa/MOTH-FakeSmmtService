variable "lambda_vehicle_recalls_api_publish" {
  type    = "string"
  default = "true"
}

variable "lambda_vehicle_recalls_api_memory_size" {
  type    = "string"
  //TODO set correct value
  default = "768"
}

variable "lambda_vehicle_recalls_api_timeout" {
  type    = "string"
  //TODO set correct value
  default = "15"
}

variable "lambda_vehicle_recalls_api_ver" {
  type    = "string"
  default = "$LATEST"
}

variable "lambda_vehicle_recalls_api_s3_key" {
  type    = "string"
  default = "fakeSmmtService.zip"
}

variable "lambda_vehicle_recalls_api_handler" {
  type    = "string"
  default = "src/smmtService.handler"
}