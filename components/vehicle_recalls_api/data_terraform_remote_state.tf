data "terraform_remote_state" "mgmt" {
  backend = "s3"

# vehicle-recalls-terraformscaffold-054631451206-eu-west-1
  config {
    bucket = "${format(
      "%s-terraformscaffold-%s-%s",
      'vehicle-recalls',
      '054631451206',
      'eu-west-1'
    )}"

    key = "${format(
      "%s/%s/%s/%s/%s.tfstate",
      'vehicle-recalls',
      '054631451206',
      'eu-west-1',
      'dawidm',
      'fake_smmt'
    )}"

    region = "${lookup(var.mgmt, "region")}"
  }
}
