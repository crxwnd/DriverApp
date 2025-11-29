'use client'

import { MeshTransmissionMaterial } from '@react-three/drei'
import { Color } from 'three'

type SmokedGlassMaterialProps = {
  color?: string
  attenuationColor?: string
  thickness?: number
  roughness?: number
  ior?: number
  chromaticAberration?: number
  transmission?: number
}

export function SmokedGlassMaterial({
  color = '#ffffff',
  attenuationColor = '#1a1a1a',
  thickness = 2.0,
  roughness = 0.25,
  ior = 1.5,
  chromaticAberration = 0.06,
  transmission = 1,
}: SmokedGlassMaterialProps) {
  return (
    <MeshTransmissionMaterial
      backside={true}
      samples={10}
      resolution={1024}
      transmission={transmission}
      roughness={roughness}
      thickness={thickness}
      ior={ior}
      chromaticAberration={chromaticAberration}
      anisotropy={0.1}
      distortion={0.2}
      distortionScale={0.3}
      temporalDistortion={0.1}
      attenuationDistance={0.5}
      attenuationColor={attenuationColor}
      color={color}
    />
  )
}
