import '@/styles/globals.css'
import Scene from '@/components/canvas/Scene'
import SmoothScroll from '@/components/dom/SmoothScroll'
import { LayoutRef } from '@/components/layout/LayoutRef'

export const metadata = {
  title: 'Crownd | Portfolio',
  description: 'High-Ticket Automation Portfolio',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className="antialiased">
      <body>
        <SmoothScroll>
          <LayoutRef>
            {/* The main content layout */}
            {children}

            {/* The Global Canvas is mounted here */}
            {/* We will pass the ref from LayoutRef via a Context or similar,
                but to keep it simple in this architecture, Scene is a client component
                that can accept a ref if we structure it carefully.
                However, usually Scene and the scrolling container are siblings.
            */}
          </LayoutRef>
        </SmoothScroll>
      </body>
    </html>
  )
}
