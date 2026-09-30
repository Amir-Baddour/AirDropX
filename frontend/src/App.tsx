import { lazy, Suspense } from 'react'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'sonner'
import { Loader2 } from 'lucide-react'
import { ApiError } from '@/lib/api'
import RequireAuth from '@/features/auth/RequireAuth'

// Public pages load separately from the dashboard, so claimants download less.
const LandingPage = lazy(() => import('@/features/landing/LandingPage'))
const DocsPage = lazy(() => import('@/features/landing/DocsPage'))
const NotFoundPage = lazy(() => import('@/features/landing/NotFoundPage'))
const LoginPage = lazy(() => import('@/features/auth/LoginPage'))
const AuthCallbackPage = lazy(() => import('@/features/auth/AuthCallbackPage'))
const ClaimPage = lazy(() => import('@/features/public/ClaimPage'))
const StatusPage = lazy(() => import('@/features/public/StatusPage'))
const AppLayout = lazy(() => import('@/components/layout/AppLayout'))
const AirdropsPage = lazy(() => import('@/features/dashboard/AirdropsPage'))
const CompanyPage = lazy(() => import('@/features/dashboard/CompanyPage'))
const AirdropPage = lazy(() => import('@/features/airdrop/AirdropPage'))

const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (err) => {
      // Session expired mid-use: send the user to sign in
      if (err instanceof ApiError && err.isAuth && !location.pathname.startsWith('/login')) {
        location.assign(`/login?next=${encodeURIComponent(location.pathname)}`)
      }
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 15_000,
      refetchOnWindowFocus: true,
      retry: (count, err) => !(err instanceof ApiError && err.status >= 400 && err.status < 500) && count < 2,
    },
  },
})

const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  { path: '/docs', element: <DocsPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/auth/callback', element: <AuthCallbackPage /> },
  { path: '/claim/:id', element: <ClaimPage /> },
  { path: '/status/:token', element: <StatusPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        path: '/app',
        element: <AppLayout />,
        children: [
          { index: true, element: <AirdropsPage /> },
          { path: 'company', element: <CompanyPage /> },
          { path: 'airdrops/:id', element: <AirdropPage /> },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
])

const Fallback = () => (
  <div className="flex min-h-dvh items-center justify-center"><Loader2 className="size-6 animate-spin text-primary" /></div>
)

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Suspense fallback={<Fallback />}>
        <RouterProvider router={router} />
      </Suspense>
      <Toaster richColors closeButton position="bottom-right" theme="system" toastOptions={{ style: { whiteSpace: 'pre-line' } }} />
    </QueryClientProvider>
  )
}
