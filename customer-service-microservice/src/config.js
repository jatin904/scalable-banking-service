const dotenv = require("dotenv");
dotenv.config();

const config = {
  port: process.env.PORT || 3000,
  mongoUri: process.env.MONGO_URI || "mongodb://localhost:27017/customerdb",
  accountServiceUrl:
    process.env.ACCOUNT_SERVICE_URL || "http://accounting-service:8085",
  notificationServiceUrl:
    process.env.NOTIFICATION_SERVICE_URL || "http://mock-notification:4002",
  serviceName: process.env.SERVICE_NAME || "customer-service",
};

module.exports = config;
