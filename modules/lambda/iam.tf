data "template_file" "lambda_assumerole_policy" {
  template = "${file("${path.module}/../iam_policies/lambda_assumerole_policy.json.tpl")}"
}

resource "aws_iam_role" "lambda" {
  name               = "${var.lambda_function_name}-${var.environment}"
  assume_role_policy = "${data.template_file.lambda_assumerole_policy.rendered}"
}