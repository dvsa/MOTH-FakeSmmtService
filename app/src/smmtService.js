const serverless = require('serverless-http');
const express = require('express');
const bodyParser = require('body-parser');

const apiKeyVerifier = require('./lib/apiKeyVerifier');
const vehicles = require('./lib/vehicles');
const fakeResponse = require('./lib/fakeResponse');
const path = require('./lib/path');
const bodyParametersFormatter = require('./lib/bodyParametersFormatter');

const app = express();
app.use(bodyParser.json());
app.use(bodyParametersFormatter.middleware);
app.use(apiKeyVerifier.middleware);
app.disable('x-powered-by');

app.post(path.serviceAvailabilityPath, (req, res) => {
  res.status(200).send(fakeResponse.serviceAvailability);
});

app.post(path.marquePath, (req, res) => {
  res.status(200).send(fakeResponse.marque);
});

app.post(path.vinCheckPath, (req, res) => {
  res.status(200).send(vehicles.getRecall(req.body.vin, req.body.marque));
});

exports.app = app;
exports.handler = serverless(app);
