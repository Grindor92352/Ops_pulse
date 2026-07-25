import { PrismaClient } from '@opspulse/prisma-client';
import bcrypt from 'bcryptjs';

const prisma = new PrismaClient();

async function main() {
  console.log('🌱 Seeding OpsPulse database...');

  // 1. Create Organization
  const org = await prisma.organization.upsert({
    where: { id: 'org_default_opspulse' },
    update: {},
    create: {
      id: 'org_default_opspulse',
      name: 'OpsPulse Enterprise',
      plan: 'ENTERPRISE',
    },
  });
  console.log('✅ Created Organization:', org.name);

  const hashedPassword = await bcrypt.hash('Password123!', 10);

  // 2. Create Admin
  const admin = await prisma.user.upsert({
    where: { email: 'admin@opspulse.io' },
    update: {
      passwordHash: hashedPassword,
      status: 'ACTIVE',
      role: 'ADMIN',
    },
    create: {
      email: 'admin@opspulse.io',
      name: 'OpsPulse Admin',
      passwordHash: hashedPassword,
      role: 'ADMIN',
      status: 'ACTIVE',
      emailVerifiedAt: new Date(),
      orgId: org.id,
    },
  });
  console.log('✅ Created Admin user:', admin.email);

  // 3. Create Manager
  const manager = await prisma.user.upsert({
    where: { email: 'manager@opspulse.io' },
    update: {
      passwordHash: hashedPassword,
      status: 'ACTIVE',
      role: 'MANAGER',
    },
    create: {
      email: 'manager@opspulse.io',
      name: 'OpsPulse Manager',
      passwordHash: hashedPassword,
      role: 'MANAGER',
      status: 'ACTIVE',
      emailVerifiedAt: new Date(),
      orgId: org.id,
    },
  });
  console.log('✅ Created Manager user:', manager.email);

  // 4. Create Developer
  const dev = await prisma.user.upsert({
    where: { email: 'dev@opspulse.io' },
    update: {
      passwordHash: hashedPassword,
      status: 'ACTIVE',
      role: 'DEVELOPER',
    },
    create: {
      email: 'dev@opspulse.io',
      name: 'OpsPulse Developer',
      passwordHash: hashedPassword,
      role: 'DEVELOPER',
      status: 'ACTIVE',
      emailVerifiedAt: new Date(),
      orgId: org.id,
    },
  });
  console.log('✅ Created Developer user:', dev.email);

  // 5. Create Default Project
  const project = await prisma.project.upsert({
    where: { id: 'proj_default_opspulse' },
    update: {},
    create: {
      id: 'proj_default_opspulse',
      name: 'Payment Service Core',
      description: 'Core microservice for handling global user transactions and checkout flows.',
      sdkApiKey: 'opspulse_sk_demo_key_12345',
      orgId: org.id,
    },
  });
  console.log('✅ Created Project:', project.name);

  console.log('🎉 Seeding completed successfully!');
}

main()
  .catch((e) => {
    console.error('❌ Seeding failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
