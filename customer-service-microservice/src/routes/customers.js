const express = require("express");
const router = express.Router();
const controller = require("../controllers/customerController");

router.post("/", controller.createCustomer);
router.get("/", controller.listCustomers);
router.get("/:id", controller.getCustomer);
router.put("/:id", controller.updateCustomer);
router.delete("/:id", controller.deleteCustomer);

// Bulk CSV upload
router.post(
  "/bulk-upload",
  controller.uploadMiddleware,
  controller.bulkCreateCustomers
);

router.get("/health", (req, res) => {
  res.status(200).json({
    status: "UP",
    service: "customer-service",
    timestamp: new Date().toISOString(),
  });
});

module.exports = router;
