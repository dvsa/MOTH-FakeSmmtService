module "s3" {
  source                    = "/var/lib/jenkins/workspace/Recalls/recalls-build/recalls-terraform/s3"
  project                   = "${var.project}"                                # has default value
  environment               = "${var.environment}"
  bucket_prefix             = "${var.bucket_prefix}"                          # has default value
  bucket_versioning_enabled = "${var.bucket_versioning_enabled}"              # has default value
}

output "s3_location" {
  value = "${module.s3.s3_location}"
}