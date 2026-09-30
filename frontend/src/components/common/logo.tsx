import { Link } from 'react-router'
import { cn } from '@/lib/utils'

export function Logo({ className, to = '/' }: { className?: string; to?: string }) {
  return (
    <Link to={to} className={cn('inline-flex items-center gap-2 font-semibold tracking-tight', className)}>
      <img src="/logo.svg" alt="" className="size-7" />
      <span>Airdrop<span className="brand-text">X</span></span>
    </Link>
  )
}
