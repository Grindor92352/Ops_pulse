import "dotenv/config";
import { sendMail } from "../src/lib/mailer";

async function main() {
  console.log("📧 Sending test email via Gmail SMTP to sudhirsrajawat2005@gmail.com...");
  const result = await sendMail({
    to: "sudhirsrajawat2005@gmail.com",
    subject: "🎉 OpsPulse Gmail SMTP Integration Verified!",
    html: `
      <div style="font-family: Arial, sans-serif; background-color: #09090b; color: #ffffff; padding: 30px; border-radius: 12px;">
        <h1 style="color: #6366f1; margin-bottom: 10px;">OpsPulse SMTP Connected!</h1>
        <p style="color: #a1a1aa; font-size: 16px;">
          Your OpsPulse incident management platform is now configured to send real emails via Gmail SMTP.
        </p>
        <div style="background-color: #18181b; padding: 15px; border-radius: 8px; border: 1px solid #27272a; margin-top: 20px;">
          <p style="margin: 0; color: #22c55e; font-weight: bold;">✔ Account Verification Emails Active</p>
          <p style="margin: 5px 0 0 0; color: #22c55e; font-weight: bold;">✔ Team Invites & SLA Breach Alerts Active</p>
        </div>
      </div>
    `
  });
  console.log("✅ Email Dispatch Status:", result);
}

main().catch(console.error);
