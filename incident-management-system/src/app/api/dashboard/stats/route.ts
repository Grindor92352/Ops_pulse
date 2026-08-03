import { prisma } from "@/lib/db";
import { Prisma } from "@opspulse/prisma-client";
import { withAuth, apiResponse, apiError } from "@/lib/api-utils";
import { formatTimeAgo } from "@/lib/utils";
import { redis } from "@/lib/redis";
import { logger } from "@/lib/logger";

// ── Cache Configuration ──────────────────────────────────────────────────────
// Dashboard stats are cached in Redis for CACHE_TTL_SECONDS.
// With 100 concurrent users hitting this endpoint, caching reduces
// DB queries from 400/min (100 users × 4 queries × SWR refresh) to ~8/min.
const CACHE_TTL_SECONDS = 2;

export const GET = withAuth(async (req, { decoded }) => {
  const { orgId } = decoded;
  if (!orgId) return apiError("Organization is required", 403);

  // ── Build role-based filters ───────────────────────────────────────────
  let cacheKey = `dashboard:stats:${orgId}`;
  const baseWhere: Prisma.IssueWhereInput = {
    project: { orgId }
  };
  const recentIssuesWhere: Prisma.IssueWhereInput = {
    project: { orgId }
  };

  if (decoded.role === 'DEVELOPER') {
    cacheKey = `dashboard:stats:${orgId}:developer:${decoded.userId}`;
    baseWhere.assignedToId = decoded.userId;
    recentIssuesWhere.assignedToId = decoded.userId;
  } else if (decoded.role === 'MANAGER' && decoded.projectId) {
    cacheKey = `dashboard:stats:${orgId}:manager:${decoded.projectId}`;
    baseWhere.projectId = decoded.projectId;
    recentIssuesWhere.projectId = decoded.projectId;
  }

  // ── Try cache first ────────────────────────────────────────────────────
  if (redis) {
    try {
      const cached = await redis.get(cacheKey);
      if (cached) {
        const data = typeof cached === 'string' ? JSON.parse(cached) : cached;
        return apiResponse("Stats fetched successfully (cached)", data);
      }
    } catch (err) {
      logger.error({ err }, "Redis cache read error, falling through to DB");
    }
  }

  // ── Fetch from Spring Boot Backend (Primary) ───────────────────────────
  const springBootUrl = process.env.NEXT_PUBLIC_SPRING_BOOT_URL || "http://localhost:8080";
  try {
    const sbRes = await fetch(`${springBootUrl}/api/dashboard/stats${decoded.projectId ? `?projectId=${decoded.projectId}` : ''}`, {
      headers: { "Content-Type": "application/json" },
      next: { revalidate: 2 }
    });
    if (sbRes.ok) {
      const sbJson = await sbRes.json();
      if (sbJson.data) {
        const statsData = sbJson.data;
        const recentIssues = (statsData.recentIssues || []).map((issue: any) => ({
          id: issue.id,
          title: issue.title,
          rootCause: issue.rootCause || (issue.description ? issue.description.substring(0, 100) + '...' : "—"),
          description: issue.description,
          status: issue.status,
          severity: issue.severity,
          teamName: issue.teamName || "—",
          assignedToEmail: issue.assignedToEmail || "—",
          timeAgo: formatTimeAgo(new Date(issue.createdAt || Date.now())),
          logs: issue.logs,
          createdAt: issue.createdAt,
          projectId: issue.projectId,
          teamId: issue.teamId,
          source: issue.source,
          project: issue.projectName ? { id: issue.projectId, name: issue.projectName } : null
        }));

        const responseData = {
          stats: {
            openIssuesCount: statsData.openIssues || 0,
            breachedCount: (statsData.responseSlaBreaches || 0) + (statsData.resolutionSlaBreaches || 0),
            resolvedTodayCount: statsData.resolvedIssues || 0,
          },
          recentIssues
        };

        return apiResponse("Stats fetched successfully from Spring Boot", responseData);
      }
    }
  } catch (sbErr) {
    logger.error({ err: sbErr }, "Spring Boot API unreachable, falling back to local DB");
  }

  // ── Fallback: Fetch from DB (parallelized) ───────────────────────────────
  const [openIssuesCount, breachedCount, resolvedTodayCount, recentIssuesRaw] = await Promise.all([
    prisma.issue.count({ 
      where: { 
        ...baseWhere,
        status: { in: ['OPEN', 'ASSIGNED', 'IN_PROGRESS'] }
      } 
    }),
    prisma.issue.count({ 
      where: { 
        ...baseWhere,
        OR: [{ responseBreached: true }, { resolutionBreached: true }]
      } 
    }),
    prisma.issue.count({ 
      where: { 
        ...baseWhere,
        status: 'RESOLVED', 
        resolvedAt: { gte: new Date(new Date().setHours(0,0,0,0)) }
      } 
    }),
    prisma.issue.findMany({
      where: recentIssuesWhere,
      take: 20,
      orderBy: { createdAt: 'desc' },
      include: { team: true, assignedTo: true, project: { select: { id: true, name: true } } }
    }),
  ]);

  const recentIssues = recentIssuesRaw.map((issue) => ({
    id: issue.id,
    title: issue.title,
    rootCause: issue.description.substring(0, 100) + '...',
    description: issue.description,
    status: issue.status,
    severity: issue.severity,
    teamName: issue.team?.name || "—",
    assignedToEmail: issue.assignedTo?.email || "—",
    timeAgo: formatTimeAgo(new Date(issue.createdAt)),
    logs: issue.logs as Record<string, unknown> | null,
    createdAt: issue.createdAt,
    projectId: issue.projectId,
    teamId: issue.teamId,
    source: issue.source,
    project: issue.project ? { id: issue.project.id, name: issue.project.name } : null
  }));

  const responseData = {
    stats: {
      openIssuesCount,
      breachedCount,
      resolvedTodayCount,
    },
    recentIssues
  };

  // ── Write to cache ─────────────────────────────────────────────────────
  if (redis) {
    try {
      await redis.set(cacheKey, JSON.stringify(responseData), { ex: CACHE_TTL_SECONDS });
    } catch (err) {
      logger.error({ err }, "Redis cache write error");
    }
  }

  return apiResponse("Stats fetched successfully", responseData);
});
