import crypto from "crypto";

const apiKey = "opspulse_sk_demo_key_12345";
const payload = {
  message: "Unhandled NullPointerException in CheckoutWorker: User cart is null",
  stack: "Error: User cart is null\n at CheckoutWorker.process (src/checkout.ts:44:12)",
  severity: "HIGH",
  environment: "PRODUCTION"
};

async function run() {
  console.log("Sending request to http://localhost:3000/api/ingest...");
  const res = await fetch("http://localhost:3000/api/ingest", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${apiKey}`
    },
    body: JSON.stringify(payload)
  });

  const data = await res.json();
  console.log("Response Status:", res.status);
  console.log("Response Data:", data);
}

run().catch(console.error);
