// Shapes returned by the Spark API (see backend/docs/AIRDROP_API.md and CLAIM_API.md).

export type AirdropStatus = 'DRAFT' | 'VALIDATED' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED'
export type RecipientStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
export type ClaimStatus = 'APPROVED' | 'NEEDS_REVIEW' | 'REJECTED'
export type TaskType = 'QUIZ' | 'SECRET_CODE' | 'MANUAL_PROOF'

export interface Company {
  id: string
  name: string
  role?: string
  created_at?: string
}

export interface Airdrop {
  id: string
  company_id: string
  name: string
  description: string | null
  token_symbol: string
  status: AirdropStatus
  created_by: string
  created_at: string
  updated_at: string
  launched_at: string | null
  completed_at: string | null
  claims_open: boolean
  claim_amount: string | null
  max_claims: number | null
  stats?: AirdropStats
}

export interface AirdropStats {
  total_recipients: number
  total_amount: string
  by_status: Partial<Record<RecipientStatus, number>>
}

export interface Recipient {
  id: string
  address: string
  amount: string
  status: RecipientStatus
  tx_ref: string | null
  error: string | null
  updated_at: string
}

export interface AirdropEvent {
  id: number
  type: string
  message: string
  created_at: string
}

export interface QuizQuestion {
  question: string
  options: string[]
  answer?: number
}

export interface Task {
  id: string
  type: TaskType
  title: string
  description: string | null
  position: number
  auto_verified: boolean
  config: {
    questions?: QuizQuestion[]
    hint?: string
    code_hash?: string
    instructions?: string
  }
}

export interface TaskResult {
  task_id: string
  passed: boolean
  needs_review: boolean
  proof: string | null
  detail: string | null
}

export interface Claim {
  id: string
  address: string
  status: ClaimStatus
  reject_reason: string | null
  created_at: string
  reviewed_at: string | null
  results: TaskResult[]
}

export interface ClaimList {
  counts: Record<ClaimStatus, number>
  items: Claim[]
}

export interface ClaimSettings {
  airdrop_id: string
  claims_open: boolean
  claim_amount: string | null
  max_claims: number | null
  public_path: string
}

export interface PublicAirdrop {
  id: string
  name: string
  description: string | null
  token_symbol: string
  claim_amount: string | null
  max_claims: number | null
  tasks: Task[]
}

export interface ClaimSubmitted {
  claim_id: string
  status: ClaimStatus
  claim_token: string
  status_path: string
}

export interface PublicClaimStatus {
  airdrop_name: string
  token_symbol: string
  address: string
  status: ClaimStatus
  reject_reason: string | null
  payout_status: RecipientStatus | null
  tx_ref: string | null
}

export interface SessionUser {
  id: string
  username: string
  pfp: string | null
  role?: { id: string; name: string } | string
}
