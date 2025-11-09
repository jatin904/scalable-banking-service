import express from 'express';
import { router as accountRouter } from './accountRoutes.js'; // FIX: Now uses named import

const app = express();
const PORT = process.env.PORT || 8085;

// Middleware
app.use(express.json());

// --- Mount Router and Start Server ---

// Mount the router with the '/api/v1' base path
app.use('/api/v1', accountRouter);

app.listen(PORT,"0.0.0.0", () => {
    console.log(`🚀 Accounting Service running on http://0.0.0.0:${PORT}`);
});