'use client'

import { Canvas } from '@react-three/fiber'
import { View, Preload } from '@react-three/drei'
import * as THREE from 'three'

type SceneProps = {
  eventSource?: React.RefObject<HTMLElement | null>
  children?: React.ReactNode
}

export default function Scene({ eventSource, children }: SceneProps) {
  return (
    <Canvas
      className="webgl-overlay"
      eventSource={eventSource as React.MutableRefObject<HTMLElement>}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        width: '100vw',
        height: '100vh',
        pointerEvents: 'none',
        zIndex: 1
      }}
      gl={{
        antialias: true,
        toneMapping: THREE.ACESFilmicToneMapping,
        outputColorSpace: THREE.SRGBColorSpace,
        alpha: true
      }}
      dpr={[1, 1.5]}
    >
      <View.Port />
      <Preload all />
      {children}
    </Canvas>
  )
}
