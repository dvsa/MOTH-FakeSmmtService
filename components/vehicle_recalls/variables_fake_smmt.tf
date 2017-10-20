variable "lambda_fake_smmt_publish" {
  type    = "string"
  default = "true"
}

variable "lambda_fake_smmt_memory_size" {
  type    = "string"
  //TODO set correct value
  default = "768"
}

variable "lambda_fake_smmt_timeout" {
  type    = "string"
  //TODO set correct value
  default = "15"
}

variable "lambda_fake_smmt_ver" {
  type    = "string"
  default = "$LATEST"
}

variable "lambda_fake_smmt_s3_key" {
  type    = "string"
  default = "fakeSmmtService.zip"
}

variable "lambda_fake_smmt_handler" {
  type    = "string"
  default = "src/smmtService.handler"
}