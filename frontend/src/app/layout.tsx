import './globals.css';
import React from 'react';

export const metadata = {
  title: 'Collaborative C++ Code Editor',
  description: 'Real-time collaborative C++ code editor powered by Monaco, Yjs, Spring Boot, and Docker',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <body className="bg-dark-900 text-gray-100 min-h-screen antialiased">
        {children}
      </body>
    </html>
  );
}
