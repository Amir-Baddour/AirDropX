import { Link } from 'react-router'
import { Button } from '@/components/ui/button'
import { GlowBackground } from '@/components/common/glow-background'

export default function NotFoundPage() {
  return (
    <div className="relative flex min-h-dvh flex-col items-center justify-center gap-4 px-4 text-center">
      <GlowBackground />
      <p className="brand-text text-7xl font-semibold">404</p>
      <p className="text-muted-foreground">This page doesn't exist.</p>
      <Button asChild><Link to="/">Go home</Link></Button>
    </div>
  )
}
