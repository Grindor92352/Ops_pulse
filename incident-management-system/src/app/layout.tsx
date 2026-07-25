import type { Metadata } from "next";
// Removed next/font/google imports due to network fetch errors during build
import "./globals.css";
import { ToastProvider } from "@/components/ui/ToastProvider";
import { CsrfInit } from "@/components/dashboard/shared/CsrfInit";
import { NetworkStatus } from "@/components/ui/NetworkStatus";

export const metadata: Metadata = {
  title: {
    template: '%s | OpsPulse',
    default: 'OpsPulse - AI Incident Management System',
  },
  description: 'OpsPulse — Modern AI-driven Incident Management System and SRE Engine.',
  keywords: ['incident management', 'sre', 'ai root cause', 'devops', 'error tracking', 'opspulse'],
  authors: [{ name: "OpsPulse Team" }],
  openGraph: {
    title: 'OpsPulse - AI Incident Management',
    description: 'Modern AI-driven Incident Management System and SRE Engine.',
    url: process.env.NEXT_PUBLIC_APP_URL || 'https://opspulse.io',
    siteName: 'OpsPulse',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'OpsPulse - AI Incident Management',
    description: 'Modern AI-driven Incident Management System and SRE Engine.',
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`font-sans h-full antialiased`}
    >
      <body className="min-h-full flex flex-col bg-glow-spotlight">
        <CsrfInit />
        <NetworkStatus />
        <ToastProvider>{children}</ToastProvider>
      </body>
    </html>
  );
}
