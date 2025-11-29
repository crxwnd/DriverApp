'use client'

import View from '@/components/canvas/View'
import { GlassItem } from '@/components/canvas/GlassItem'

export default function Page() {
  return (
    <main className="w-full">
      {/* Hero Section */}
      <section className="h-screen w-full flex flex-col items-center justify-center p-12 border-b border-white/10">
        <h1 className="text-9xl font-thin tracking-tighter mix-blend-difference mb-8">
          CROWND
        </h1>
        <p className="text-xl text-neutral-400 tracking-widest uppercase text-center max-w-2xl">
          High-Ticket Automation • Creative Engineering • Future Systems
        </p>
      </section>

      {/* Grid Section */}
      <section className="min-h-screen p-12 grid grid-cols-1 md:grid-cols-3 gap-8 w-full">
        {['01', '02', '03'].map((id) => (
          <div
            key={id}
            className="relative h-[600px] w-full rounded-2xl border border-white/10 bg-white/5 backdrop-blur-sm overflow-hidden group"
          >
            {/* 3D View Layer */}
            <div className="absolute inset-0 z-0">
               <View className="w-full h-full">
                 <GlassItem />
               </View>
            </div>

            {/* Content Layer */}
            <div className="relative z-10 w-full h-full p-8 flex flex-col justify-between pointer-events-none">
              <div className="flex justify-between items-start">
                <span className="text-xs border border-white/20 px-2 py-1 rounded-full">
                  PROJECT / {id}
                </span>
              </div>
              <div>
                <h2 className="text-4xl font-light mb-2 translate-y-4 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition-all duration-500 ease-out">
                  System {id}
                </h2>
                <p className="text-neutral-400 text-sm translate-y-4 opacity-0 group-hover:translate-y-0 group-hover:opacity-100 transition-all duration-500 delay-75 ease-out">
                  Automated revenue architecture for enterprise clients.
                </p>
              </div>
            </div>
          </div>
        ))}
      </section>

      {/* Footer */}
      <footer className="h-[50vh] flex items-center justify-center border-t border-white/10">
        <span className="text-neutral-600">© 2025 Crownd Automation</span>
      </footer>
    </main>
  )
}
