const { v4: uuidv4 } = require("uuid");
const multer = require("multer");
const csv = require("csv-parser");
const fs = require("fs");
const Customer = require("../models/customer.model");
const external = require("../services/externalClients");
const { randomInt } = require("crypto");
 
// ===== Multer upload setup =====
const upload = multer({ dest: "uploads/" });
 
// ===== Create a single customer =====
async function createCustomer(req, res) {
  try {
    const { name, email, phone, kyc_status, metadata } = req.body;
    if (!name || !email) {
      return res.status(400).json({ error: "name and email required" });
    }
 
    const customer = new Customer({
      customer_id: uuidv4(),
      name,
      email,
      phone,
      kyc_status: kyc_status || "PENDING",
      metadata,
    });
    await customer.save();
 
    // 🏦 Create Account in Account Service
    await external.createAccountForCustomer({
      customer_id: customer.customer_id,
      account_number: String(Math.floor(100000000000000 + Math.random() * 900000000000000)),
      account_type: "SAVINGS",
      initial_deposit: 10000.0,
    });
 
    // 🔔 Send Notification
    await external.sendNotification({
      accountNumber: customer.customer_id,
      customerName: customer.name,
      customerEmail: customer.email,
      customerPhone: customer.phone,
      eventType: "CUSTOMER_CREATED",
      description: `Customer ${customer.name} created.`,
    });
 
    return res.status(201).json(customer);
  } catch (err) {
    console.error("❌ Create Customer Error:", err);
    if (err.code === 11000)
      return res.status(409).json({ error: "customer exists" });
    return res.status(500).json({ error: "internal_error" });
  }
}
 
// ===== Get single customer =====
async function getCustomer(req, res) {
  try {
    const id = req.params.id;
    const c = await Customer.findOne({ customer_id: id }).lean();
    if (!c) return res.status(404).json({ error: "not_found" });
    return res.json(c);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "internal_error" });
  }
}
 
// ===== Update customer =====
async function updateCustomer(req, res) {
  try {
    const id = req.params.id;
    const updates = req.body;
    const c = await Customer.findOneAndUpdate(
      { customer_id: id },
      { $set: updates },
      { new: true }
    ).lean();
    if (!c) return res.status(404).json({ error: "not_found" });
 
    // Notify if KYC updated
    if (updates.kyc_status) {
      await external.sendNotification({
        type: "KYC_UPDATED",
        customer_id: c.customer_id,
        message: `KYC status updated to ${updates.kyc_status}`,
      });
    }
 
    return res.json(c);
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "internal_error" });
  }
}
 
// ===== Delete customer =====
async function deleteCustomer(req, res) {
  try {
    const id = req.params.id;
    const r = await Customer.findOneAndDelete({ customer_id: id }).lean();
    if (!r) return res.status(404).json({ error: "not_found" });
 
    await external.sendNotification({
      type: "CUSTOMER_DELETED",
      customer_id: r.customer_id,
      message: `Customer ${r.name} deleted`,
    });
 
    return res.json({ deleted: true });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "internal_error" });
  }
}
 
// ===== List customers =====
async function listCustomers(req, res) {
  try {
    const page = Math.max(0, parseInt(req.query.page) || 0);
    const limit = Math.min(100, parseInt(req.query.limit) || 25);
    const docs = await Customer.find()
      .skip(page * limit)
      .limit(limit)
      .lean();
    return res.json({ count: docs.length, customers: docs });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "internal_error" });
  }
}
 
// ===== Bulk upload (CSV) =====
const uploadMiddleware = upload.single("file");
 
async function bulkCreateCustomers(req, res) {
  try {
    if (!req.file)
      return res.status(400).json({ message: "Please upload a CSV file" });
 
    const filePath = req.file.path;
    const customers = [];
 
    fs.createReadStream(filePath)
      .pipe(csv())
      .on("data", (row) => {
        customers.push({
          customer_id: row.customer_id || uuidv4(),
          name: row.name,
          email: row.email,
          phone: row.phone,
          kyc_status: row.kyc_status || "PENDING",
        });
      })
      .on("end", async () => {
        try {
          const inserted = await Customer.insertMany(customers, {
            ordered: false,
          });
          fs.unlinkSync(filePath); // clean temp file
 
          // Optional best-effort notifications
          for (const c of inserted) {
            await external.sendNotification({
              type: "CUSTOMER_CREATED",
              customer_id: c.customer_id,
              message: `Customer ${c.name} created via bulk upload.`,
            });
          }
 
          res.status(201).json({
            message: `✅ Successfully inserted ${inserted.length} customers`,
            insertedCount: inserted.length,
          });
        } catch (err) {
          console.error("Insert error:", err);
          res
            .status(500)
            .json({ message: "❌ Bulk insert failed", error: err.message });
        }
      });
  } catch (error) {
    console.error("Upload error:", error);
    res.status(500).json({ message: "❌ Upload failed", error: error.message });
  }
}
 
module.exports = {
  createCustomer,
  getCustomer,
  updateCustomer,
  deleteCustomer,
  listCustomers,
  bulkCreateCustomers,
  uploadMiddleware,
};
 
 


// const { v4: uuidv4 } = require("uuid");
// const multer = require("multer");
// const csv = require("csv-parser");
// const fs = require("fs");
// const Customer = require("../models/customer.model");
// const external = require("../services/externalClients");

// // ===== Multer upload setup =====
// const upload = multer({ dest: "uploads/" });

// // ===== Create a single customer =====
// async function createCustomer(req, res) {
//   try {
//     const { name, email, phone, kyc_status, metadata } = req.body;
//     if (!name || !email) {
//       return res.status(400).json({ error: "name and email required" });
//     }

//     const customer = new Customer({
//       customer_id: uuidv4(),
//       name,
//       email,
//       phone,
//       kyc_status: kyc_status || "PENDING",
//       metadata,
//     });
//     await customer.save();

//     // 🏦 Create Account in Account Service
//     await external.createAccountForCustomer({
//       customer_id: customer.customer_id,
//       name: customer.name,
//     });

//     // 🔔 Send Notification
//     await external.sendNotification({
//       type: "CUSTOMER_CREATED",
//       customer_id: customer.customer_id,
//       email: customer.email,
//       phone: customer.phone,
//       message: `Customer ${customer.name} created.`,
//     });

//     return res.status(201).json(customer);
//   } catch (err) {
//     console.error("❌ Create Customer Error:", err);
//     if (err.code === 11000)
//       return res.status(409).json({ error: "customer exists" });
//     return res.status(500).json({ error: "internal_error" });
//   }
// }

// // ===== Get single customer =====
// async function getCustomer(req, res) {
//   try {
//     const id = req.params.id;
//     const c = await Customer.findOne({ customer_id: id }).lean();
//     if (!c) return res.status(404).json({ error: "not_found" });
//     return res.json(c);
//   } catch (err) {
//     console.error(err);
//     return res.status(500).json({ error: "internal_error" });
//   }
// }

// // ===== Update customer =====
// async function updateCustomer(req, res) {
//   try {
//     const id = req.params.id;
//     const updates = req.body;
//     const c = await Customer.findOneAndUpdate(
//       { customer_id: id },
//       { $set: updates },
//       { new: true }
//     ).lean();
//     if (!c) return res.status(404).json({ error: "not_found" });

//     // Notify if KYC updated
//     if (updates.kyc_status) {
//       await external.sendNotification({
//         type: "KYC_UPDATED",
//         customer_id: c.customer_id,
//         message: `KYC status updated to ${updates.kyc_status}`,
//       });
//     }

//     return res.json(c);
//   } catch (err) {
//     console.error(err);
//     return res.status(500).json({ error: "internal_error" });
//   }
// }

// // ===== Delete customer =====
// async function deleteCustomer(req, res) {
//   try {
//     const id = req.params.id;
//     const r = await Customer.findOneAndDelete({ customer_id: id }).lean();
//     if (!r) return res.status(404).json({ error: "not_found" });

//     await external.sendNotification({
//       type: "CUSTOMER_DELETED",
//       customer_id: r.customer_id,
//       message: `Customer ${r.name} deleted`,
//     });

//     return res.json({ deleted: true });
//   } catch (err) {
//     console.error(err);
//     return res.status(500).json({ error: "internal_error" });
//   }
// }

// // ===== List customers =====
// async function listCustomers(req, res) {
//   try {
//     const page = Math.max(0, parseInt(req.query.page) || 0);
//     const limit = Math.min(100, parseInt(req.query.limit) || 25);
//     const docs = await Customer.find()
//       .skip(page * limit)
//       .limit(limit)
//       .lean();
//     return res.json({ count: docs.length, customers: docs });
//   } catch (err) {
//     console.error(err);
//     return res.status(500).json({ error: "internal_error" });
//   }
// }

// // ===== Bulk upload (CSV) =====
// const uploadMiddleware = upload.single("file");

// async function bulkCreateCustomers(req, res) {
//   try {
//     if (!req.file)
//       return res.status(400).json({ message: "Please upload a CSV file" });

//     const filePath = req.file.path;
//     const customers = [];

//     fs.createReadStream(filePath)
//       .pipe(csv())
//       .on("data", (row) => {
//         customers.push({
//           customer_id: row.customer_id || uuidv4(),
//           name: row.name,
//           email: row.email,
//           phone: row.phone,
//           kyc_status: row.kyc_status || "PENDING",
//         });
//       })
//       .on("end", async () => {
//         try {
//           const inserted = await Customer.insertMany(customers, {
//             ordered: false,
//           });
//           fs.unlinkSync(filePath); // clean temp file

//           // Optional best-effort notifications
//           for (const c of inserted) {
//             await external.sendNotification({
//               type: "CUSTOMER_CREATED",
//               customer_id: c.customer_id,
//               message: `Customer ${c.name} created via bulk upload.`,
//             });
//           }

//           res.status(201).json({
//             message: `✅ Successfully inserted ${inserted.length} customers`,
//             insertedCount: inserted.length,
//           });
//         } catch (err) {
//           console.error("Insert error:", err);
//           res
//             .status(500)
//             .json({ message: "❌ Bulk insert failed", error: err.message });
//         }
//       });
//   } catch (error) {
//     console.error("Upload error:", error);
//     res.status(500).json({ message: "❌ Upload failed", error: error.message });
//   }
// }

// module.exports = {
//   createCustomer,
//   getCustomer,
//   updateCustomer,
//   deleteCustomer,
//   listCustomers,
//   bulkCreateCustomers,
//   uploadMiddleware,
// };
