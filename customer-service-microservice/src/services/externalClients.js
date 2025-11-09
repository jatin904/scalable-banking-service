const axios = require("axios");

const ACCOUNT_SERVICE_URL =
  process.env.ACCOUNT_SERVICE_URL || "http://accounting-service:8085";
const NOTIFICATION_SERVICE_URL =
  process.env.NOTIFICATION_SERVICE_URL || "http://localhost:8038";

/**
 * Call Account Service
 */
async function createAccountForCustomer(payload) {
  try {
    console.log("➡️ [CustomerService] Creating account for:", payload);
    console.log(ACCOUNT_SERVICE_URL);
    const response = await axios.post(
      `${ACCOUNT_SERVICE_URL}/api/v1/accounts`,
      payload
    );
    console.log("✅ Account created:", response.data);
    return response.data;
  } catch (err) {
    console.log("Error in create customer ",err)
    console.warn("⚠️ Account Service unavailable:", err.message);
    return null;
  }
}

/**
 * Call Notification Service
 */
async function sendNotification(payload) {
  try {
    console.log("➡️ [CustomerService] Sending notification:", payload);
    const response = await axios.post(
      `${NOTIFICATION_SERVICE_URL}/api/notifications/accounts/events`,
      payload
    );
    console.log("✅ Notification sent:", response.data);
    return response.data;
  } catch (err) {
    console.warn("⚠️ Notification Service unavailable:", err.message);
    return null;
  }
}

module.exports = { createAccountForCustomer, sendNotification };
