module "s3" {
  source                    = "../../modules/s3"
  project                   = "${var.project}"                                # has default value
  environment               = "${var.environment}"
  bucket_prefix             = "${var.bucket_prefix}"                          # has default value
  bucket_versioning_enabled = "${var.bucket_versioning_enabled}"              # has default value
}