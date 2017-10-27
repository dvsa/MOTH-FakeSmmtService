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