/**
 * Budget Tab Component
 * Shows budget breakdown, spending analysis, and cost visualizations
 * Task 19: Mobile-optimized with horizontal scroll for tables and responsive charts
 */

import { useMemo } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { DollarSign, TrendingUp, TrendingDown, AlertCircle } from 'lucide-react';
import { useItinerary } from '@/hooks/useItinerary';
import { useMediaQuery } from '@/hooks/useMediaQuery';

interface BudgetTabProps {
  tripId: string;
}

export function BudgetTab({ tripId }: BudgetTabProps) {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const { data: itinerary } = useItinerary(tripId);
  
  // Calculate real budget data from itinerary
  const budgetData = useMemo(() => {
    if (!itinerary) {
      return { total: 0, spent: 0, remaining: 0, currency: 'USD' };
    }
    
    // NormalizedItinerary has days with nodes directly
    const total = itinerary.days.reduce((sum, day) => sum + (day.totals?.cost || 0), 0);
    const spent = itinerary.days.reduce((sum, day) => 
      sum + (day.nodes || []).filter(n => n.bookingRef).reduce((s, n) => s + (n.cost?.amount || 0), 0), 0
    );
    
    return {
      total,
      spent,
      remaining: total - spent,
      currency: itinerary.currency || 'USD',
    };
  }, [itinerary]);
  
  // Calculate category breakdown
  const categoryData = useMemo(() => {
    if (!itinerary) return [];
    
    const categories: Record<string, number> = {};
    itinerary.days.forEach(day => {
      (day.nodes || []).forEach((node: any) => {
        const category = (node.type === 'accommodation' || node.type === 'hotel') ? 'Accommodation' :
                        (node.type === 'transport' || node.type === 'transit') ? 'Transportation' :
                        node.type === 'meal' ? 'Food & Dining' :
                        (node.type === 'attraction' || node.type === 'activity') ? 'Activities' : 'Other';
        categories[category] = (categories[category] || 0) + (node.cost?.amount || 0);
      });
    });
    
    const colors = {
      'Accommodation': '#002B5B',
      'Transportation': '#F5C542',
      'Food & Dining': '#10B981',
      'Activities': '#F59E0B',
      'Other': '#EF4444',
    };
    
    return Object.entries(categories).map(([name, value]) => ({
      name,
      value,
      color: colors[name as keyof typeof colors] || '#999',
    }));
  }, [itinerary]);
  
  // Calculate daily costs
  const dailyCosts = useMemo(() => {
    if (!itinerary) return [];
    
    return itinerary.days.map(day => ({
      day: `Day ${day.dayNumber}`,
      cost: day.totals?.cost || 0,
    }));
  }, [itinerary]);
  
  if (!itinerary) {
    return <div className="p-8 text-center text-muted-foreground">Loading budget data...</div>;
  }
  
  const percentageSpent = budgetData.total > 0 ? (budgetData.spent / budgetData.total) * 100 : 0;
  const isOverBudget = budgetData.spent > budgetData.total;

  return (
    <div className="space-y-4 sm:space-y-6 p-3 sm:p-6">
      {/* Budget Overview */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3 sm:gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4 sm:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">Total Budget</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <div className="text-xl sm:text-2xl font-bold">
              ${budgetData.total.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              {budgetData.currency}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4 sm:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">Spent</CardTitle>
            <TrendingUp className="h-4 w-4 text-error" />
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <div className="text-xl sm:text-2xl font-bold text-error">
              ${budgetData.spent.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              {percentageSpent.toFixed(1)}% of budget
            </p>
          </CardContent>
        </Card>

        <Card className="sm:col-span-2 lg:col-span-1">
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-4 sm:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">Remaining</CardTitle>
            <TrendingDown className="h-4 w-4 text-success" />
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <div className={`text-xl sm:text-2xl font-bold ${isOverBudget ? 'text-error' : 'text-success'}`}>
              ${Math.abs(budgetData.remaining).toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              {isOverBudget ? 'Over budget' : 'Under budget'}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Budget Alert */}
      {percentageSpent > 80 && (
        <Card className="border-warning bg-warning/5">
          <CardContent className="flex items-start gap-3 p-4 sm:pt-6">
            <AlertCircle className="h-4 w-4 sm:h-5 sm:w-5 text-warning flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-sm sm:text-base font-medium text-warning">Budget Alert</p>
              <p className="text-xs sm:text-sm text-muted-foreground">
                You've spent {percentageSpent.toFixed(1)}% of your budget. Consider adjusting your spending.
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Category Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle>Spending by Category</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Pie Chart */}
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={(entry: any) => `${entry.name} ${(entry.percent * 100).toFixed(0)}%`}
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(value) => `$${value}`} />
                </PieChart>
              </ResponsiveContainer>
            </div>

            {/* Category List */}
            <div className="space-y-3">
              {categoryData.map((category) => (
                <div key={category.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div
                      className="w-4 h-4 rounded-full"
                      style={{ backgroundColor: category.color }}
                    />
                    <span className="text-sm font-medium">{category.name}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold">${category.value}</span>
                    <Badge variant="outline" className="text-xs">
                      {((category.value / budgetData.spent) * 100).toFixed(0)}%
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Daily Costs */}
      <Card>
        <CardHeader>
          <CardTitle>Daily Spending</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={dailyCosts}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" />
                <YAxis />
                <Tooltip formatter={(value) => `$${value}`} />
                <Legend />
                <Bar dataKey="cost" fill="#002B5B" name="Daily Cost" />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-4 text-sm text-muted-foreground">
            Average daily cost: ${(budgetData.spent / dailyCosts.length).toFixed(2)}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
