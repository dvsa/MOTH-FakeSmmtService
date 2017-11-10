data "terraform_remote_state" "fake_smmt" {
  backend = "s3"

  config {
    # bucket = "vehicle-recalls-terraformscaffold-054631451206-eu-west-1"
    bucket = "${format(
          "%s-terraformscaffold-%s-%s",
          var.project,
          var.account_id,
          data.aws_region.current.name
        )}"
    # key = "vehicle-recalls/054631451206/eu-west-1/int/fake_smmt.tfstate"
    key = "${format(
      "%s/%s/%s/%s/%s.tfstate",
      var.project,
      var.account_id,
      data.aws_region.current.name,
      var.environment,
      var.fake_smmt_component_name
    )}"

    region = "eu-west-1"
  }
}
