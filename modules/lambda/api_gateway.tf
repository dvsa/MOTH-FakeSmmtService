resource "aws_api_gateway_rest_api" "api" {
  name               = "${var.lambda_function_name}-${var.environment}"
  description        = "${var.lambda_function_name} for ${var.environment}"
}

####################################################################################################################################
# API GATEWAY ROOT RESOURCE

# ANY method -> ROOT (/)
resource "aws_api_gateway_method" "api_root_any" {
  rest_api_id      = "${aws_api_gateway_rest_api.api.id}"
  resource_id      = "${aws_api_gateway_rest_api.api.root_resource_id}"
  http_method      = "ANY"
  authorization    = "NONE"
  api_key_required = "${length(var.api_rate_limit_vars) > 0}"
}

# integration between ROOT resource's ANY method and Lambda function (back-end)
resource "aws_api_gateway_integration" "api_root_any" {
  rest_api_id             = "${aws_api_gateway_rest_api.api.id}"
  resource_id             = "${aws_api_gateway_rest_api.api.root_resource_id}"
  http_method             = "${aws_api_gateway_method.api_root_any.http_method}"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${var.aws_region}:lambda:path/2015-03-31/functions/${aws_lambda_function.lambda.arn}:${aws_lambda_alias.lambda_alias.name}/invocations"
  integration_http_method = "POST"
}

resource "aws_api_gateway_method_response" "api_root_any_200" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  http_method = "${aws_api_gateway_method.api_root_any.http_method}"
  status_code = "200"

  response_models = {
    "application/json" = "Empty"
  }

  response_parameters = {
    "method.response.header.Date"           = true
    "method.response.header.ETag"           = true
    "method.response.header.Content-Length" = true
    "method.response.header.Content-Type"   = true
    "method.response.header.Last-Modified"  = true
  }
}

resource "aws_api_gateway_integration_response" "api_root_any_200" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_rest_api.api.root_resource_id}"
  http_method = "${aws_api_gateway_method.api_root_any.http_method}"
  status_code = "${aws_api_gateway_method_response.api_root_any_200.status_code}"

  response_parameters = {
    "method.response.header.Content-Length" = "integration.response.header.Content-Length"
    "method.response.header.Content-Type"   = "integration.response.header.Content-Type"
    "method.response.header.Date"           = "integration.response.header.Date"
    "method.response.header.ETag"           = "integration.response.header.ETag"
    "method.response.header.Last-Modified"  = "integration.response.header.Last-Modified"
  }

  depends_on = ["aws_api_gateway_integration.api_root_any"]
}

####################################################################################################################################
# API GATEWAY LAMBDA WILDCARD RESOURCE
resource "aws_api_gateway_resource" "api_wildcard" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  parent_id   = "${aws_api_gateway_rest_api.api.root_resource_id}"
  path_part   = "{proxy+}"
}

# ANY method -> Lambda Wildcard/{proxy}
resource "aws_api_gateway_method" "api_wildcard_any" {
  rest_api_id      = "${aws_api_gateway_rest_api.api.id}"
  resource_id      = "${aws_api_gateway_resource.api_wildcard.id}"
  http_method      = "ANY"
  authorization    = "NONE"
  api_key_required = "${length(var.api_rate_limit_vars) > 0}"
}

# integration between Lambda Wildcard resource's POST method and Lambda function (back-end)
resource "aws_api_gateway_integration" "api_wildcard_any" {
  rest_api_id             = "${aws_api_gateway_rest_api.api.id}"
  resource_id             = "${aws_api_gateway_resource.api_wildcard.id}"
  http_method             = "${aws_api_gateway_method.api_wildcard_any.http_method}"
  type                    = "AWS_PROXY"
  uri                     = "arn:aws:apigateway:${var.aws_region}:lambda:path/2015-03-31/functions/${aws_lambda_function.lambda.arn}:${aws_lambda_alias.lambda_alias.name}/invocations"
  integration_http_method = "POST"
}

resource "aws_api_gateway_method_response" "api_wildcard_any_200" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_resource.api_wildcard.id}"
  http_method = "${aws_api_gateway_method.api_wildcard_any.http_method}"
  status_code = "200"

  response_models = {
    "application/json" = "Empty"
  }

  response_parameters = {
    "method.response.header.Date"           = true
    "method.response.header.ETag"           = true
    "method.response.header.Content-Length" = true
    "method.response.header.Content-Type"   = true
    "method.response.header.Last-Modified"  = true
  }
}

resource "aws_api_gateway_integration_response" "api_wildcard_any_200" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  resource_id = "${aws_api_gateway_resource.api_wildcard.id}"
  http_method = "${aws_api_gateway_method.api_wildcard_any.http_method}"
  status_code = "${aws_api_gateway_method_response.api_wildcard_any_200.status_code}"

  response_parameters = {
    "method.response.header.Content-Length" = "integration.response.header.Content-Length"
    "method.response.header.Content-Type"   = "integration.response.header.Content-Type"
    "method.response.header.Date"           = "integration.response.header.Date"
    "method.response.header.ETag"           = "integration.response.header.ETag"
    "method.response.header.Last-Modified"  = "integration.response.header.Last-Modified"
  }

  depends_on = ["aws_api_gateway_integration.api_wildcard_any"]
}

resource "aws_api_gateway_deployment" "api" {
  rest_api_id = "${aws_api_gateway_rest_api.api.id}"
  stage_name  = "${var.environment}"

  depends_on = [
    "aws_api_gateway_method.api_root_any",
    "aws_api_gateway_integration.api_root_any",
    "aws_api_gateway_method.api_wildcard_any",
    "aws_api_gateway_integration.api_wildcard_any",
  ]
}

####################################################################################################################################
# API KEY AND USAGE PLAN

resource "aws_api_gateway_api_key" "api_key" {
  count         = "${length(var.api_rate_limit_vars) > 0 ? 1 : 0}"
  name          = "${var.lambda_function_name}-${var.environment}-api-key"
  description   = "Api key for calling ${aws_api_gateway_rest_api.api.name}"
}

resource "aws_api_gateway_usage_plan" "usage_limits" {
  count         = "${length(var.api_rate_limit_vars) > 0 ? 1 : 0}"
  name          = "${var.lambda_function_name}-${var.environment}-api-plan"
  description   = "Usage plan defining throttling for ${aws_api_gateway_rest_api.api.name}"

  api_stages {
    api_id = "${aws_api_gateway_rest_api.api.id}"
    stage  = "${aws_api_gateway_deployment.api.stage_name}"
  }

  throttle_settings {
    rate_limit  = "${var.api_rate_limit_vars["rate_limit"]}"
    burst_limit = "${var.api_rate_limit_vars["burst_limit"]}"
  }

  quota_settings {
    limit  = "${var.api_rate_limit_vars["quota_monthly"]}"
    offset = 0
    period = "MONTH"
  }

  depends_on = [
    "aws_api_gateway_deployment.api",
    "aws_api_gateway_api_key.api_key"
  ]
}

resource "aws_api_gateway_usage_plan_key" "plan_api_key" {
  count         = "${length(var.api_rate_limit_vars) > 0 ? 1 : 0}"
  key_id        = "${aws_api_gateway_api_key.api_key.id}"
  key_type      = "API_KEY"
  usage_plan_id = "${aws_api_gateway_usage_plan.usage_limits.id}"

  depends_on = [
    "aws_api_gateway_usage_plan.usage_limits"
  ]
}

output "api_gateway_url" {
  value = "https://${aws_api_gateway_deployment.api.rest_api_id}.execute-api.${var.aws_region}.amazonaws.com/${aws_api_gateway_deployment.api.stage_name}"
}

output "api_key" {
  value = "${aws_api_gateway_api_key.api_key.value}"
}