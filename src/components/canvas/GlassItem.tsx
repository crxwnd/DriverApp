'use client'

import { Float, Center, Environment } from '@react-three/drei'
import { SmokedGlassMaterial } from './materials/SmokedGlassMaterial'

export function GlassItem() {
  return (
    <>
      <Environment preset="city" />

      <Float speed={2} rotationIntensity={0.5} floatIntensity={0.5}>
        <Center>
          <mesh>
            <torusKnotGeometry args={[1, 0.3, 128, 32]} />
            <SmokedGlassMaterial />
          </mesh>
        </Center>
      </Float>

      <spotLight
        position={[10, 10, 10]}
        intensity={50}
        angle={0.2}
        penumbra={1}
        color="#ffffff"
      />
    </>
  )
}
