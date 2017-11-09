resource "aws_lambda_function" "lambda" {
  description   = "${var.lambda_function_name}"
  runtime       = "nodejs6.10"
  s3_bucket     = "${var.bucket_prefix}${var.environment}"
  s3_key        = "${var.lambda_s3_key}"
  function_name = "${var.lambda_function_name}-${var.environment}"
  role          = "${aws_iam_role.lambda.arn}"
  handler       = "${var.lambda_handler}"
  publish       = "${var.lambda_publish}"
  memory_size   = "${var.lambda_memory_size}"
  timeout       = "${var.lambda_timeout}"

  environment {
    variables = "${var.lambda_env_vars}"
  }

  depends_on = ["aws_api_gateway_rest_api.api"]
}

//we probably don't need it
resource "aws_lambda_alias" "lambda_alias" {
  name             = "${var.environment}"
  description      = "Alias for ${aws_lambda_function.lambda.function_name}"
  function_name    = "${aws_lambda_function.lambda.arn}"
  function_version = "$LATEST"
}

resource "aws_lambda_permission" "lambda_allow_apigateway" {
  function_name = "${aws_lambda_function.lambda.function_name}"
  qualifier     = "${aws_lambda_alias.lambda_alias.name}"
  statement_id  = "AllowExecutionFromApiGateway"
  action        = "lambda:InvokeFunction"
  principal     = "apigateway.amazonaws.com"
  source_arn    = "arn:aws:execute-api:${var.aws_region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.api.id}/*/*/*"

  depends_on = [
    "aws_api_gateway_rest_api.api",
    "aws_api_gateway_integration.api_root_any",
  ]
}
