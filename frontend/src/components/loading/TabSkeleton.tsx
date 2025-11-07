/**
 * Tab Skeleton Loaders
 * Shown while fetching tab-specific data
 */

import { Card, CardContent, CardHeader } from '@/components/ui/card';

/**
 * Generic Tab Skeleton
 * Used for any tab that needs a loading state
 */
export function TabSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Header Skeleton */}
      <div className="h-8 w-48 bg-muted rounded" />
      
      {/* Content Cards */}
      {[1, 2, 3].map((i) => (
        <Card key={i}>
          <CardHeader>
            <div className="h-6 w-32 bg-muted rounded" />
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              <div className="h-4 w-full bg-muted rounded" />
              <div className="h-4 w-3/4 bg-muted rounded" />
              <div className="h-4 w-1/2 bg-muted rounded" />
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

/**
 * View Tab Skeleton
 * Specific skeleton for the View tab with stats cards
 */
export function ViewTabSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {[1, 2, 3, 4].map((i) => (
          <Card key={i}>
            <CardContent className="p-6">
              <div className="space-y-2">
                <div className="h-4 w-16 bg-muted rounded" />
                <div className="h-8 w-24 bg-muted rounded" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Map Skeleton */}
      <Card>
        <CardContent className="p-0">
          <div className="h-96 bg-muted rounded" />
        </CardContent>
      </Card>

      {/* Weather Widget Skeleton */}
      <Card>
        <CardHeader>
          <div className="h-6 w-32 bg-muted rounded" />
        </CardHeader>
        <CardContent>
          <div className="flex gap-4 overflow-x-auto">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex-shrink-0 space-y-2">
                <div className="h-4 w-16 bg-muted rounded" />
                <div className="h-12 w-12 bg-muted rounded-full mx-auto" />
                <div className="h-4 w-16 bg-muted rounded" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

/**
 * Plan Tab Skeleton
 * Specific skeleton for the Plan tab with day cards
 */
export function PlanTabSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Day Cards */}
      {[1, 2, 3].map((day) => (
        <Card key={day}>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div className="space-y-2">
                <div className="h-6 w-32 bg-muted rounded" />
                <div className="h-4 w-48 bg-muted rounded" />
              </div>
              <div className="h-8 w-24 bg-muted rounded" />
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[1, 2, 3].map((activity) => (
                <div key={activity} className="flex gap-4 p-4 border rounded-lg">
                  <div className="h-12 w-12 bg-muted rounded-full flex-shrink-0" />
                  <div className="flex-1 space-y-2">
                    <div className="h-5 w-48 bg-muted rounded" />
                    <div className="h-4 w-32 bg-muted rounded" />
                    <div className="h-4 w-24 bg-muted rounded" />
                  </div>
                  <div className="h-8 w-20 bg-muted rounded" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

/**
 * Bookings Tab Skeleton
 * Specific skeleton for the Bookings tab with provider sidebar
 */
export function BookingsTabSkeleton() {
  return (
    <div className="flex gap-6 animate-pulse">
      {/* Provider Sidebar Skeleton */}
      <div className="w-64 flex-shrink-0 space-y-4">
        <div className="h-6 w-32 bg-muted rounded" />
        {[1, 2, 3].map((i) => (
          <div key={i} className="space-y-2">
            <div className="h-5 w-24 bg-muted rounded" />
            <div className="space-y-1">
              {[1, 2, 3].map((j) => (
                <div key={j} className="h-10 w-full bg-muted rounded" />
              ))}
            </div>
          </div>
        ))}
      </div>

      {/* Bookings List Skeleton */}
      <div className="flex-1 space-y-4">
        <div className="h-8 w-48 bg-muted rounded" />
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardContent className="p-6">
              <div className="flex gap-4">
                <div className="h-16 w-16 bg-muted rounded flex-shrink-0" />
                <div className="flex-1 space-y-2">
                  <div className="h-5 w-48 bg-muted rounded" />
                  <div className="h-4 w-32 bg-muted rounded" />
                  <div className="h-4 w-24 bg-muted rounded" />
                </div>
                <div className="space-y-2">
                  <div className="h-6 w-20 bg-muted rounded" />
                  <div className="h-8 w-24 bg-muted rounded" />
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

/**
 * Budget Tab Skeleton
 * Specific skeleton for the Budget tab with charts
 */
export function BudgetTabSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {[1, 2, 3].map((i) => (
          <Card key={i}>
            <CardContent className="p-6">
              <div className="space-y-2">
                <div className="h-4 w-24 bg-muted rounded" />
                <div className="h-8 w-32 bg-muted rounded" />
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <div className="h-6 w-40 bg-muted rounded" />
          </CardHeader>
          <CardContent>
            <div className="h-64 bg-muted rounded" />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <div className="h-6 w-40 bg-muted rounded" />
          </CardHeader>
          <CardContent>
            <div className="h-64 bg-muted rounded" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

/**
 * Packing Tab Skeleton
 */
export function PackingTabSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {[1, 2, 3].map((category) => (
        <Card key={category}>
          <CardHeader>
            <div className="h-6 w-32 bg-muted rounded" />
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {[1, 2, 3, 4].map((item) => (
                <div key={item} className="flex items-center gap-3">
                  <div className="h-5 w-5 bg-muted rounded" />
                  <div className="h-4 w-48 bg-muted rounded" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}

/**
 * Docs Tab Skeleton
 */
export function DocsTabSkeleton() {
  return (
    <div className="space-y-6 animate-pulse">
      {[1, 2, 3, 4].map((section) => (
        <Card key={section}>
          <CardHeader>
            <div className="h-6 w-40 bg-muted rounded" />
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {[1, 2].map((doc) => (
                <div key={doc} className="flex items-center gap-4 p-4 border rounded-lg">
                  <div className="h-12 w-12 bg-muted rounded flex-shrink-0" />
                  <div className="flex-1 space-y-2">
                    <div className="h-5 w-48 bg-muted rounded" />
                    <div className="h-4 w-32 bg-muted rounded" />
                  </div>
                  <div className="h-8 w-24 bg-muted rounded" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
