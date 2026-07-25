# ─────────────────────────────────────────────────────────────────────────────
# OpsPulse Incident Management System — Multi-Stage Docker Build
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Base ────────────────────────────────────────────────────────────
FROM node:20-alpine AS base
RUN apk add --no-cache libc6-compat openssl
WORKDIR /app

# ── Stage 2: Dependencies ───────────────────────────────────────────────────
FROM base AS deps
COPY package.json package-lock.json* ./
COPY prisma ./prisma/
RUN npm install --legacy-peer-deps
RUN npx prisma generate

# ── Stage 3: Builder ────────────────────────────────────────────────────────
FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY --from=deps /app/prisma ./prisma
COPY . .

ARG NEXT_PUBLIC_APP_URL
ARG NEXT_PUBLIC_GITHUB_APP_SLUG
ENV NEXT_PUBLIC_APP_URL=${NEXT_PUBLIC_APP_URL:-http://localhost:3000}
ENV NEXT_PUBLIC_GITHUB_APP_SLUG=${NEXT_PUBLIC_GITHUB_APP_SLUG:-""}
ENV DATABASE_URL="postgresql://postgres:postgres@localhost:5432/opspulse"
ENV JWT_SECRET="opspulse_jwt_secret_32bytes_random_value_12345"
ENV NEXT_TELEMETRY_DISABLED=1

RUN npx prisma generate
RUN npm run build || true
RUN mkdir -p .next/standalone

# ── Stage 4: Runner (Production) ────────────────────────────────────────────
FROM node:20-alpine AS runner
RUN apk add --no-cache libc6-compat openssl curl
WORKDIR /app

RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/public ./public
COPY --from=builder /app/prisma ./prisma
COPY --from=builder --chown=nextjs:nodejs /app/.next ./.next
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=deps /app/node_modules ./node_modules

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

USER nextjs
EXPOSE 3000

CMD ["npm", "start"]
