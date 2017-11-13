output "fake_smmt_api_gateway_url" {
  value = "${module.fake_smmt.api_gateway_url}"
}

output "vehicle_recalls_api_gateway_url" {
  value = "${module.vehicle_recalls_api.api_gateway_url}"
}

output "vehicle_recalls_api_key" {
  value = "${module.vehicle_recalls_api.api_key}"
}