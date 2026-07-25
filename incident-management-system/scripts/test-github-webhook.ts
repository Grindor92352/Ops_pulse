import crypto from "crypto";

const secret = process.env.GITHUB_WEBHOOK_SECRET || "opspulse_github_secret_12345";
const payload = JSON.stringify({
  action: "completed",
  workflow_run: {
    name: "Production Build & Test Suite",
    conclusion: "failure",
    html_url: "https://github.com/opspulse/payment-service/actions/runs/987654"
  },
  repository: {
    html_url: "https://github.com/opspulse/payment-service"
  }
});

const hmac = crypto.createHmac("sha256", secret);
const signature = "sha256=" + hmac.update(payload).digest("hex");

async function run() {
  const res = await fetch("http://localhost:3000/api/webhooks/github", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-GitHub-Event": "workflow_run",
      "X-Hub-Signature-256": signature
    },
    body: payload
  });

  const data = await res.json();
  console.log("✅ GitHub Webhook Test Result:", res.status, data);
}

run().catch(console.error);
