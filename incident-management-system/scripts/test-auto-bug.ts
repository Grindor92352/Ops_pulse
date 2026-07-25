import { OpsPulse, IssueSeverity } from '../../sdk';

async function runTest() {
  OpsPulse.init({
    apiKey: 'opspulse_sk_demo_key_12345',
    baseUrl: 'http://localhost:3000/api/ingest',
    flushInterval: 1000,
  });

  const uniqueId = Date.now();
  console.log(`⚡ [Client App] App started (Run ID: ${uniqueId})...`);

  // Simulating an unhandled Payment Gateway Timeout bug
  try {
    throw new Error(`Payment Gateway Timeout after 30000ms at CheckoutWorker.processPayment (Ref #${uniqueId})`);
  } catch (crashError: any) {
    console.log("💥 CRASH OCCURRED! Capturing automatically via OpsPulse SDK...");

    await OpsPulse.captureException(crashError, {
      severity: IssueSeverity.CRITICAL,
      tags: {
        component: "CheckoutWorker",
        environment: "PRODUCTION",
        runId: String(uniqueId)
      }
    });

    console.log("⏳ Flushing report queue to server...");
    await OpsPulse.flush();

    console.log("✅ Crash reported and flushed to OpsPulse! Refresh your Admin Dashboard now.");
  }
}

runTest().catch(console.error);
