resource "aws_iam_role" "lambda" {
  name               = "${var.lambda_function_name}-${var.environment}"
  assume_role_policy = "${data.template_file.assume_role_policy.rendered}"
}

resource "aws_iam_role_policy" "enable_cwlogs_policy" {
  name   = "${var.lambda_function_name}-enable-cwlogs-${var.environment}"
  role   = "${aws_iam_role.lambda.id}"
  policy = "${data.template_file.enable_cwlogs_policy.rendered}"
}
