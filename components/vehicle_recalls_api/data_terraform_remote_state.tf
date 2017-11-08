data "terraform_remote_state" "mgmt" {
  backend = "s3"

  config {
    bucket = "vehicle-recalls-terraformscaffold-054631451206-eu-west-1"
        # "${format(
        #   ,
        #   "vehicle-recalls",
        #   "054631451206",
        #   "eu-west-1"
        # )}"
    key = "vehicle-recalls/054631451206/eu-west-1/dawidm/fake_smmt.tfstate"
    # key = "${format(
    #   "%s/%s/%s/%s/%s.tfstate",
    #   "vehicle-recalls",
    #   "054631451206",
    #   "eu-west-1",
    #   "dawidm",
    #   "fake_smmt"
    # )}"

    region = "eu-west-1"
  }
}
