data "template_file" "assume_role_policy" {
  template = "${file("${path.module}/templates/lambda_assumerole_policy.json.tpl")}"
}

data "template_file" "enable_cwlogs_policy" {
  template = "${file("${path.module}/templates/lambda_enable_cwlogs_policy.json.tpl")}"

  vars {
    aws_region      = "${var.aws_region}"
    account_id      = "${data.aws_caller_identity.current.account_id}"
    lambda_function = "${aws_lambda_function.lambda.function_name}"
  }
}
