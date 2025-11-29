'use client'

import { View as ViewImpl } from '@react-three/drei'
import { forwardRef, useImperativeHandle, useRef } from 'react'

type ViewProps = {
  children?: React.ReactNode
  className?: string
}

const View = forwardRef<HTMLDivElement, ViewProps>(({ children, className, ...props }, ref) => {
  const localRef = useRef<HTMLDivElement>(null)

  useImperativeHandle(ref, () => localRef.current!)

  return (
    <>
      <div ref={localRef} className={`relative w-full h-full ${className}`} {...props} />
      <ViewImpl track={localRef as React.MutableRefObject<HTMLElement>}>
        {children}
      </ViewImpl>
    </>
  )
})

View.displayName = 'View'

export default View
