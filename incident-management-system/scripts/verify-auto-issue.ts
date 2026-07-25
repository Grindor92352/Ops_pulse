import { PrismaClient } from "@opspulse/prisma-client";

const prisma = new PrismaClient();

async function run() {
  console.log("🔍 STEP 1: Counting current issues in database...");
  const initialCount = await prisma.issue.count();
  console.log(`📊 Initial Database Issue Count: ${initialCount}`);

  const runId = Date.now();
  const payload = {
    message: `Automatic Error Ingestion Test #${runId}: OutOfMemoryError in Heap Space`,
    stack: `java.lang.OutOfMemoryError: Java heap space\n  at com.opspulse.memory.LeakDetector.allocate(LeakDetector.java:${Math.floor(Math.random() * 500) + 100})\n  at com.opspulse.service.OrderWorker.run(OrderWorker.java:42)`,
    severity: "CRITICAL",
    environment: "PRODUCTION",
    tags: { runId: String(runId), source: "Automated Verification Test" }
  };

  console.log(`\n⚡ STEP 2: Ingesting new crash report into OpsPulse API...`);
  const res = await fetch("http://localhost:3000/api/ingest", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": "Bearer opspulse_sk_demo_key_12345"
    },
    body: JSON.stringify(payload)
  });

  const responseData = await res.json();
  console.log("📡 API Response:", res.status, responseData);

  console.log("\n🔍 STEP 3: Checking updated database issue count...");
  const newCount = await prisma.issue.count();
  console.log(`📊 Updated Database Issue Count: ${newCount} (Difference: +${newCount - initialCount})`);

  console.log("\n🔥 STEP 4: Querying newly created incident from PostgreSQL:");
  const latestIssue = await prisma.issue.findFirst({
    orderBy: { createdAt: "desc" },
    include: { project: true }
  });

  console.log({
    id: latestIssue?.id,
    title: latestIssue?.title,
    severity: latestIssue?.severity,
    source: latestIssue?.source,
    project: latestIssue?.project?.name,
    createdAt: latestIssue?.createdAt,
  });

  if (newCount > initialCount && latestIssue?.id === responseData.issueId) {
    console.log("\n✅ SUCCESS: Automatic Issue Generation is 100% WORKING & VERIFIED!");
  } else {
    console.log("\n❌ FAILED: Issue was not generated.");
  }
}

run()
  .catch(console.error)
  .finally(() => prisma.$disconnect());
