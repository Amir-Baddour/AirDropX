import { Badge } from '@/components/ui/badge'
import type { AirdropStatus, ClaimStatus, RecipientStatus } from '@/lib/types'

type Tone = 'neutral' | 'info' | 'warning' | 'success' | 'danger' | 'brand'

const AIRDROP: Record<AirdropStatus, { tone: Tone; label: string }> = {
  DRAFT: { tone: 'neutral', label: 'Draft' },
  VALIDATED: { tone: 'info', label: 'Validated' },
  PROCESSING: { tone: 'warning', label: 'Processing' },
  COMPLETED: { tone: 'success', label: 'Completed' },
  CANCELLED: { tone: 'danger', label: 'Cancelled' },
}
const RECIPIENT: Record<RecipientStatus, { tone: Tone; label: string }> = {
  PENDING: { tone: 'neutral', label: 'Pending' },
  PROCESSING: { tone: 'warning', label: 'Sending' },
  COMPLETED: { tone: 'success', label: 'Paid' },
  FAILED: { tone: 'danger', label: 'Failed' },
  CANCELLED: { tone: 'danger', label: 'Cancelled' },
}
const CLAIM: Record<ClaimStatus, { tone: Tone; label: string }> = {
  APPROVED: { tone: 'success', label: 'Approved' },
  NEEDS_REVIEW: { tone: 'warning', label: 'Needs review' },
  REJECTED: { tone: 'danger', label: 'Rejected' },
}

export const AirdropStatusBadge = ({ status }: { status: AirdropStatus }) => (
  <Badge tone={AIRDROP[status].tone} dot pulse={status === 'PROCESSING'}>{AIRDROP[status].label}</Badge>
)
export const RecipientStatusBadge = ({ status }: { status: RecipientStatus }) => (
  <Badge tone={RECIPIENT[status].tone} dot pulse={status === 'PROCESSING'}>{RECIPIENT[status].label}</Badge>
)
export const ClaimStatusBadge = ({ status }: { status: ClaimStatus }) => (
  <Badge tone={CLAIM[status].tone} dot>{CLAIM[status].label}</Badge>
)
export const recipientLabel = (s: RecipientStatus) => RECIPIENT[s].label
