resource "aws_s3_bucket" "s3_bucket" {
  bucket        = "${var.bucket_prefix}${var.environment}"
  force_destroy = "true"

  versioning {
    enabled = "${var.bucket_versioning_enabled}"
  }

  tags {
    Name        = "${var.project}-${var.environment}"
    Project     = "${var.project}"
    Environment = "${var.environment}"
  }
}