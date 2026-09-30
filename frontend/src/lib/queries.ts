import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'
import type {
  AdminAction, AdminCompany, AdminStats, PlatformEvent,
  Airdrop, AirdropEvent, Claim, ClaimList, ClaimSettings, Company, PublicAirdrop,
  PublicClaimStatus, ClaimSubmitted, Recipient, Task, TaskType,
} from './types'

export const keys = {
  company: ['company'] as const,
  airdrops: ['airdrops'] as const,
  airdrop: (id: string) => ['airdrop', id] as const,
  recipients: (id: string, status?: string) => ['airdrop', id, 'recipients', status ?? 'all'] as const,
  events: (id: string) => ['airdrop', id, 'events'] as const,
  tasks: (id: string) => ['airdrop', id, 'tasks'] as const,
  claims: (id: string, status?: string) => ['airdrop', id, 'claims', status ?? 'all'] as const,
  publicAirdrop: (id: string) => ['public', 'airdrop', id] as const,
  publicClaim: (token: string) => ['public', 'claim', token] as const,
}

// ---- company ----
export const useCompany = () =>
  useQuery({ queryKey: keys.company, queryFn: () => api.get<Company>('/companies/me'), retry: false })

export function useCreateCompany() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.post<Company>('/companies', { name }),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.company }),
  })
}

// ---- airdrops ----
export const useAirdrops = () =>
  useQuery({ queryKey: keys.airdrops, queryFn: () => api.get<Airdrop[]>('/airdrops?limit=100') })

export const useAirdrop = (id: string) =>
  useQuery({
    queryKey: keys.airdrop(id),
    queryFn: () => api.get<Airdrop>(`/airdrops/${id}`),
    // Live progress while the worker is paying out
    refetchInterval: (q) => (q.state.data?.status === 'PROCESSING' ? 3000 : false),
  })

export function useCreateAirdrop() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: { name: string; token_symbol: string; description?: string }) => api.post<Airdrop>('/airdrops', body),
    onSuccess: () => qc.invalidateQueries({ queryKey: keys.airdrops }),
  })
}

export type LifecycleAction = 'validate' | 'reopen' | 'launch' | 'cancel'
export function useAirdropAction(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (action: LifecycleAction) => api.post<Airdrop>(`/airdrops/${id}/${action}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['airdrop', id] })
      qc.invalidateQueries({ queryKey: keys.airdrops })
    },
  })
}

export const useRecipients = (id: string, status?: string, poll = false) =>
  useQuery({
    queryKey: keys.recipients(id, status),
    queryFn: () => api.get<Recipient[]>(`/airdrops/${id}/recipients?limit=200${status ? `&status=${status}` : ''}`),
    refetchInterval: poll ? 3000 : false,
  })

export function useAddRecipients(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (recipients: { address: string; amount: string }[]) =>
      api.post<{ received: number; added: number; skipped_existing: number }>(`/airdrops/${id}/recipients`, { recipients }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['airdrop', id] }),
  })
}

export const useEvents = (id: string, poll = false) =>
  useQuery({
    queryKey: keys.events(id),
    queryFn: () => api.get<AirdropEvent[]>(`/airdrops/${id}/events`),
    refetchInterval: poll ? 3000 : false,
  })

// ---- tasks ----
export const useTasks = (id: string) =>
  useQuery({ queryKey: keys.tasks(id), queryFn: () => api.get<Task[]>(`/airdrops/${id}/tasks`) })

export function useCreateTask(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: { type: TaskType; title: string; description?: string; config: unknown }) =>
      api.post<Task>(`/airdrops/${id}/tasks`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['airdrop', id] }),
  })
}

export function useDeleteTask(id: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (taskId: string) => api.del(`/airdrops/${id}/tasks/${taskId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['airdrop', id] }),
  })
}

// ---- claims (company) ----
export const useClaims = (id: string, status?: string) =>
  useQuery({
    queryKey: keys.claims(id, status),
    queryFn: () => api.get<ClaimList>(`/airdrops/${id}/claims?limit=200${status ? `&status=${status}` : ''}`),
    refetchInterval: 10000,
  })

export function useClaimSettings(id: string) {
  const qc = useQueryClient()
  const done = () => {
    qc.invalidateQueries({ queryKey: ['airdrop', id] })
    qc.invalidateQueries({ queryKey: keys.airdrops })
  }
  return {
    open: useMutation({
      mutationFn: (body: { claim_amount: string; max_claims?: number | null }) =>
        api.post<ClaimSettings>(`/airdrops/${id}/claims/open`, body),
      onSuccess: done,
    }),
    close: useMutation({ mutationFn: () => api.post<ClaimSettings>(`/airdrops/${id}/claims/close`), onSuccess: done }),
  }
}

export function useReviewClaim(id: string) {
  const qc = useQueryClient()
  const done = () => qc.invalidateQueries({ queryKey: ['airdrop', id] })
  return {
    approve: useMutation({ mutationFn: (claimId: string) => api.post<Claim>(`/airdrops/${id}/claims/${claimId}/approve`), onSuccess: done }),
    reject: useMutation({
      mutationFn: ({ claimId, reason }: { claimId: string; reason: string }) =>
        api.post<Claim>(`/airdrops/${id}/claims/${claimId}/reject`, { reason }),
      onSuccess: done,
    }),
  }
}

// ---- public (no login) ----
export const usePublicAirdrop = (id: string) =>
  useQuery({ queryKey: keys.publicAirdrop(id), queryFn: () => api.get<PublicAirdrop>(`/public/airdrops/${id}`, false), retry: false })

export const useSubmitClaim = (id: string) =>
  useMutation({
    mutationFn: (body: { address: string; answers: Record<string, unknown> }) =>
      api.post<ClaimSubmitted>(`/public/airdrops/${id}/claims`, body, false),
  })

export const usePublicClaim = (token: string) =>
  useQuery({
    queryKey: keys.publicClaim(token),
    queryFn: () => api.get<PublicClaimStatus>(`/public/claims/${token}`, false),
    retry: false,
    refetchInterval: (q) => {
      const d = q.state.data
      if (!d || d.status === 'REJECTED' || d.payout_status === 'COMPLETED' || d.payout_status === 'CANCELLED') return false
      return 8000
    },
  })

// ---- platform admin (SUPERADMIN only; 403 for everyone else) ----
export const useAdminAccess = () =>
  useQuery({ queryKey: ['admin', 'access'], queryFn: () => api.get<{ admin: boolean }>('/admin/access'), retry: false, staleTime: 5 * 60_000 })

export const useAdminStats = () =>
  useQuery({ queryKey: ['admin', 'stats'], queryFn: () => api.get<AdminStats>('/admin/stats'), refetchInterval: 30_000 })

export const useAdminCompanies = (q: string, status: string) =>
  useQuery({
    queryKey: ['admin', 'companies', q, status],
    queryFn: () => {
      const params = new URLSearchParams({ limit: '200' })
      if (q) params.set('q', q)
      if (status) params.set('status', status)
      return api.get<AdminCompany[]>(`/admin/companies?${params}`)
    },
    placeholderData: (prev) => prev,
  })

export const useAdminAudit = () =>
  useQuery({ queryKey: ['admin', 'audit'], queryFn: () => api.get<AdminAction[]>('/admin/audit?limit=100') })

export const useAdminActivity = () =>
  useQuery({ queryKey: ['admin', 'activity'], queryFn: () => api.get<PlatformEvent[]>('/admin/activity?limit=30'), refetchInterval: 30_000 })

export function useCompanyModeration() {
  const qc = useQueryClient()
  const done = () => qc.invalidateQueries({ queryKey: ['admin'] })
  return {
    suspend: useMutation({
      mutationFn: ({ id, reason }: { id: string; reason: string }) => api.post(`/admin/companies/${id}/suspend`, { reason }),
      onSuccess: done,
    }),
    restore: useMutation({
      mutationFn: ({ id, note }: { id: string; note?: string }) => api.post(`/admin/companies/${id}/restore`, note ? { note } : {}),
      onSuccess: done,
    }),
  }
}
