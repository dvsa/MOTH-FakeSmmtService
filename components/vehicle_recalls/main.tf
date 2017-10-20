module "VehicleRecalls" {
  source                                 = "github.com/dvsa/vehicle-recalls-terraform.git?ref=feature-BL-6181-pipeline"
  aws_region                             = "${var.aws_region}"                             # has default value
  project                                = "${var.project}"                                # has default value
  environment                            = "${var.environment}"
  bucket_prefix                          = "${var.bucket_prefix}"                          # has default value
  bucket_versioning_enabled              = "${var.bucket_versioning_enabled}"              # has default value

  //VEHICLE RECALLS API LAMBDA
  lambda_vehicle_recalls_api_publish     = "${var.lambda_vehicle_recalls_api_publish}"     # has default value
  lambda_vehicle_recalls_api_memory_size = "${var.lambda_vehicle_recalls_api_memory_size}" # has default value
  lambda_vehicle_recalls_api_timeout     = "${var.lambda_vehicle_recalls_api_timeout}"     # has default value
  lambda_vehicle_recalls_api_ver         = "${var.lambda_vehicle_recalls_api_ver}"         # has default value
  lambda_vehicle_recalls_api_s3_key      = "${var.lambda_vehicle_recalls_api_s3_key}"      # has default value
  lambda_vehicle_recalls_api_handler     = "${var.lambda_vehicle_recalls_api_handler}"     # has default value

  //FAKE SMMT LAMBDA
  lambda_fake_smmt_publish     = "${var.lambda_fake_smmt_publish}"     # has default value
  lambda_fake_smmt_memory_size = "${var.lambda_fake_smmt_memory_size}" # has default value
  lambda_fake_smmt_timeout     = "${var.lambda_fake_smmt_timeout}"     # has default value
  lambda_fake_smmt_ver         = "${var.lambda_fake_smmt_ver}"         # has default value
  lambda_fake_smmt_s3_key      = "${var.lambda_fake_smmt_s3_key}"      # has default value
  lambda_fake_smmt_handler     = "${var.lambda_fake_smmt_handler}"     # has default value

}