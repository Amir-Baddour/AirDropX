import { Navigate, Outlet, useLocation } from 'react-router'
import { useSession } from '@/lib/auth'

export default function RequireAuth() {
  const { isAuthenticated } = useSession()
  const location = useLocation()
  if (!isAuthenticated) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname)}`} replace />
  return <Outlet />
}
