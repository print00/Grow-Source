import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "TableProof P&L",
  description: "Restaurant profit and loss dashboard"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
