/**
 * Booking Status Badge Component
 * Displays booking status with appropriate styling
 */

import { cn } from '@/lib/utils';
import { CheckCircle, Clock, XCircle, AlertCircle } from 'lucide-react';

type BookingStatus = 'confirmed' | 'pending' | 'cancelled' | 'failed';

interface BookingStatusBadgeProps {
  status: BookingStatus;
  className?: string;
}

const statusConfig = {
  confirmed: {
    label: 'Confirmed',
    icon: CheckCircle,
    className: 'bg-success/10 text-success border-success/20',
  },
  pending: {
    label: 'Pending',
    icon: Clock,
    className: 'bg-warning/10 text-warning border-warning/20',
  },
  cancelled: {
    label: 'Cancelled',
    icon: XCircle,
    className: 'bg-muted text-muted-foreground border-border',
  },
  failed: {
    label: 'Failed',
    icon: AlertCircle,
    className: 'bg-error/10 text-error border-error/20',
  },
};

export function BookingStatusBadge({ status, className }: BookingStatusBadgeProps) {
  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border',
        config.className,
        className
      )}
    >
      <Icon className="w-3.5 h-3.5" />
      {config.label}
    </span>
  );
}
