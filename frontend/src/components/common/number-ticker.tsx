import { animate, useInView, useMotionValue } from 'motion/react'
import { useEffect, useRef } from 'react'

/** Counts up to `value` when it scrolls into view (Magic UI style). */
export function NumberTicker({ value, decimals = 0, className }: { value: number; decimals?: number; className?: string }) {
  const ref = useRef<HTMLSpanElement>(null)
  const inView = useInView(ref, { once: true })
  const mv = useMotionValue(0)

  useEffect(() => {
    if (!inView) return
    const controls = animate(mv, value, { duration: 0.9, ease: 'easeOut' })
    return () => controls.stop()
  }, [inView, value, mv])

  useEffect(
    () =>
      mv.on('change', (v) => {
        if (ref.current) ref.current.textContent = v.toLocaleString(undefined, { maximumFractionDigits: decimals, minimumFractionDigits: decimals })
      }),
    [mv, decimals],
  )

  return <span ref={ref} className={className}>0</span>
}
