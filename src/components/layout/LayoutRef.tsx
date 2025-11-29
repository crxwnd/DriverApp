'use client'

import { useRef } from 'react'
import Scene from '@/components/canvas/Scene'

export function LayoutRef({ children }: { children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null)

  return (
    <div ref={ref} className="relative w-full min-h-screen bg-black text-neutral-200">
      <div className="relative z-10">
        {children}
      </div>
      <Scene eventSource={ref} />
    </div>
  )
}
