const mongoose = require("mongoose");

const CustomerSchema = new mongoose.Schema({
  customer_id: { type: String, required: true, unique: true }, // e.g., uuid
  name: { type: String, required: true },
  email: { type: String, required: true, index: true },
  phone: { type: String },
  kyc_status: {
    type: String,
    enum: ["PENDING", "VERIFIED", "REJECTED"],
    default: "PENDING",
  },
  metadata: { type: Object, default: {} },
  created_at: { type: Date, default: Date.now },
  updated_at: { type: Date, default: Date.now },
});

CustomerSchema.pre("save", function (next) {
  this.updated_at = Date.now();
  next();
});

module.exports = mongoose.model("Customer", CustomerSchema);
