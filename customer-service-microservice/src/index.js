const express = require("express");
const bodyParser = require("body-parser");
const morgan = require("morgan");
const helmet = require("helmet");
const mongoose = require("mongoose");
const cors = require("cors");
const config = require("./config");
const customers = require("./routes/customers");
const swagger = require("./swagger.json");
const YAML = require("yamljs");

const app = express();

app.use(helmet());
app.use(cors());
app.use(bodyParser.json());
app.use(morgan("combined"));

// health
app.get("/health", (req, res) =>
  res.json({ status: "UP", service: config.serviceName })
);

// swagger
app.get("/openapi.json", (req, res) => res.json(swagger));

// routes
app.use("/customers", customers);

// global error handler
app.use((err, req, res, next) => {
  console.error("Unhandled error", err);
  res.status(500).json({ error: "internal_error" });
});

async function start() {
  console.log("Connecting to Mongo at", config.mongoUri);
  await mongoose.connect(config.mongoUri, {
    useNewUrlParser: true,
    useUnifiedTopology: true,
  });
  console.log("Mongo connected");

  app.listen(config.port, () => {
    console.log(`Customer service listening on ${config.port}`);
  });
}

start().catch((err) => {
  console.error("Failed to start", err);
  process.exit(1);
});
