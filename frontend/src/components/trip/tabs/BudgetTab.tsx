/**
 * Budget Tab Component
 * Shows budget breakdown, spending analysis, and cost visualizations
 * Task 19: Mobile-optimized with horizontal scroll for tables and responsive charts
 */

import { useMemo, useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { DollarSign, TrendingUp, TrendingDown, AlertCircle } from 'lucide-react';
import { useItinerary } from '@/hooks/useItinerary';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { api, endpoints } from '@/services/api';
import { useTranslation } from '@/i18n';
import { NestedBudgetPieChart } from '../charts/NestedBudgetPieChart';

interface BudgetTabProps {
  tripId: string;
}

export function BudgetTab({ tripId }: BudgetTabProps) {
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width: 768px)');
  const { data: itinerary } = useItinerary(tripId);
  const [metadata, setMetadata] = useState<any>(null);
  
  // Fetch trip metadata to get user's original budget
  useEffect(() => {
    const fetchMetadata = async () => {
      try {
        const data = await api.get(endpoints.getMetadata(tripId));
        setMetadata(data);
      } catch (error) {
        console.error('Failed to fetch trip metadata:', error);
      }
    };
    
    fetchMetadata();
  }, [tripId]);
  
  // Get user's original budget preferences from metadata
  const userBudget = useMemo(() => {
    if (!metadata) {
      return {
        min: 0,
        max: 0,
        tier: '',
      };
    }
    
    return {
      min: metadata.budgetMin || 0,
      max: metadata.budgetMax || 0,
      tier: metadata.budgetTier || '',
    };
  }, [metadata]);
  
  // Calculate real budget data from itinerary
  const budgetData = useMemo(() => {
    if (!itinerary) {
      return { 
        total: 0, 
        spent: 0, 
        remaining: 0, 
        currency: 'USD',
        plannedBudget: userBudget.max || 0,
      };
    }
    
    const currency = itinerary.currency || 'USD';
    
    // Calculate total from day totalCost (backend field)
    const total = itinerary.days.reduce((sum, day: any) => sum + (day.totalCost || 0), 0);
    
    // Calculate spent from booked nodes
    const spent = itinerary.days.reduce((sum, day) => 
      sum + (day.nodes || [])
        .filter((n: any) => n.bookingRef)
        .reduce((s, n: any) => s + (n.cost?.amountPerPerson || n.cost?.amount || 0), 0), 
      0
    );
    
    return {
      total,
      spent,
      remaining: total - spent,
      currency,
      plannedBudget: userBudget.max || total,
    };
  }, [itinerary, userBudget]);
  
  // Calculate category breakdown
  const categoryData = useMemo(() => {
    if (!itinerary) return [];
    
    const categories: Record<string, number> = {};
    itinerary.days.forEach(day => {
      (day.nodes || []).forEach((node: any) => {
        const category = node.type === 'accommodation' ? 'Accommodation' :
                        node.type === 'transport' ? 'Transportation' :
                        node.type === 'meal' ? 'Food & Dining' :
                        (node.type === 'activity' || node.type === 'place' || node.type === 'attraction') ? 'Activities' : 'Other';
        const cost = node.cost?.amountPerPerson || node.cost?.amount || 0;
        categories[category] = (categories[category] || 0) + cost;
      });
    });
    
    const colors = {
      'Accommodation': '#002B5B',
      'Transportation': '#F5C542',
      'Food & Dining': '#10B981',
      'Activities': '#F59E0B',
      'Other': '#EF4444',
    };
    
    // Define consistent order for categories
    const categoryOrder = ['Accommodation', 'Food & Dining', 'Activities', 'Transportation', 'Other'];
    
    return categoryOrder
      .filter(name => categories[name] > 0)
      .map(name => ({
        name,
        value: categories[name],
        color: colors[name as keyof typeof colors] || '#999',
      }));
  }, [itinerary]);

  // Calculate per-day category breakdown for nested chart
  const dayBreakdown = useMemo(() => {
    if (!itinerary) return [];
    
    return itinerary.days.map((day: any) => {
      const categories: Record<string, number> = {
        'Accommodation': 0,
        'Transportation': 0,
        'Food & Dining': 0,
        'Activities': 0,
        'Other': 0,
      };
      
      (day.nodes || []).forEach((node: any) => {
        const category = node.type === 'accommodation' ? 'Accommodation' :
                        node.type === 'transport' ? 'Transportation' :
                        node.type === 'meal' ? 'Food & Dining' :
                        (node.type === 'activity' || node.type === 'place' || node.type === 'attraction') ? 'Activities' : 'Other';
        const cost = node.cost?.amountPerPerson || node.cost?.amount || 0;
        categories[category] += cost;
      });
      
      return {
        day: day.dayNumber,
        categories,
      };
    });
  }, [itinerary]);
  
  // Calculate daily costs
  const dailyCosts = useMemo(() => {
    if (!itinerary) return [];
    
    return itinerary.days.map((day: any) => ({
      day: `Day ${day.dayNumber}`,
      cost: day.totalCost || 0,
    }));
  }, [itinerary]);
  
  if (!itinerary) {
    return <div className="p-8 text-center text-muted-foreground">{t('components.budgetTab.loading')}</div>;
  }
  
  const percentageSpent = budgetData.plannedBudget > 0 ? (budgetData.spent / budgetData.plannedBudget) * 100 : 0;
  const isOverBudget = budgetData.spent > budgetData.plannedBudget;
  const isOverPlannedBudget = budgetData.total > budgetData.plannedBudget;

  return (
    <div className="space-y-3 sm:space-y-4 md:space-y-6">
      {/* Budget Overview */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-2 sm:gap-3 md:gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">{t('components.budgetTab.cards.yourBudget.title')}</CardTitle>
            <DollarSign className="h-3 w-3 sm:h-4 sm:w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
            <div className="text-lg sm:text-xl md:text-2xl font-bold">
              {budgetData.currency} {budgetData.plannedBudget.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
              {userBudget.tier ? t('components.budgetTab.cards.yourBudget.tier', { tier: userBudget.tier }) : 'Planned budget'}
            </p>
          </CardContent>
        </Card>
        
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">{t('components.budgetTab.cards.estimatedCost.title')}</CardTitle>
            <DollarSign className="h-3 w-3 sm:h-4 sm:w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
            <div className={`text-lg sm:text-xl md:text-2xl font-bold ${isOverPlannedBudget ? 'text-warning' : ''}`}>
              {budgetData.currency} {budgetData.total.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
              {isOverPlannedBudget ? t('components.budgetTab.cards.estimatedCost.overBudget') : t('components.budgetTab.cards.estimatedCost.withinBudget')}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">{t('components.budgetTab.cards.booked.title')}</CardTitle>
            <TrendingUp className="h-3 w-3 sm:h-4 sm:w-4 text-error" />
          </CardHeader>
          <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
            <div className="text-lg sm:text-xl md:text-2xl font-bold text-error">
              {budgetData.currency} {budgetData.spent.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
              {t('components.budgetTab.cards.booked.percentage', { percentage: percentageSpent.toFixed(1) })}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">{t('components.budgetTab.cards.remaining.title')}</CardTitle>
            <TrendingDown className="h-3 w-3 sm:h-4 sm:w-4 text-success" />
          </CardHeader>
          <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
            <div className={`text-lg sm:text-xl md:text-2xl font-bold ${isOverBudget ? 'text-error' : 'text-success'}`}>
              {budgetData.currency} {Math.abs(budgetData.plannedBudget - budgetData.spent).toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
              {isOverBudget ? t('components.budgetTab.cards.remaining.overBudget') : t('components.budgetTab.cards.remaining.available')}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Budget Alert */}
      {percentageSpent > 80 && (
        <Card className="border-warning bg-warning/5">
          <CardContent className="flex items-start gap-2 sm:gap-3 p-3 sm:p-4 md:pt-6">
            <AlertCircle className="h-4 w-4 sm:h-5 sm:w-5 text-warning flex-shrink-0 mt-0.5" />
            <div className="min-w-0 flex-1">
              <p className="text-xs sm:text-sm md:text-base font-medium text-warning">{t('components.budgetTab.alert.title')}</p>
              <p className="text-xs sm:text-sm text-muted-foreground">
                {t('components.budgetTab.alert.message', { percentage: percentageSpent.toFixed(1) })}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Category Breakdown - Nested Pie Chart */}
      <Card>
        <CardHeader className="p-3 sm:p-4 md:p-6">
          <CardTitle className="text-base sm:text-lg">{t('components.budgetTab.categoryBreakdown.title')}</CardTitle>
          <p className="text-xs text-muted-foreground mt-1">
            Inner circle shows category totals, outer circle shows per-day breakdown
          </p>
        </CardHeader>
        <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 sm:gap-6">
            {/* Nested Pie Chart */}
            <div className="h-[350px] sm:h-[400px]">
              <NestedBudgetPieChart
                categoryData={categoryData}
                days={dayBreakdown}
                currency={budgetData.currency}
              />
            </div>

            {/* Category List */}
            <div className="space-y-2 sm:space-y-3">
              {categoryData.map((category) => (
                <div key={category.name} className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 sm:gap-3 min-w-0 flex-1">
                    <div
                      className="w-3 h-3 sm:w-4 sm:h-4 rounded-full flex-shrink-0"
                      style={{ backgroundColor: category.color }}
                    />
                    <span className="text-xs sm:text-sm font-medium truncate">{category.name}</span>
                  </div>
                  <div className="flex items-center gap-1.5 sm:gap-2 flex-shrink-0">
                    <span className="text-xs sm:text-sm font-bold">{budgetData.currency} {category.value.toLocaleString()}</span>
                    <Badge variant="outline" className="text-xs">
                      {budgetData.total > 0 ? ((category.value / budgetData.total) * 100).toFixed(0) : 0}%
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
        <CardHeader className="p-3 sm:p-4 md:p-6">
          <CardTitle className="text-base sm:text-lg">{t('components.budgetTab.dailySpending.title')}</CardTitle>
        </CardHeader>
        <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
          <div className="h-[250px] sm:h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={dailyCosts}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="day" tick={{ fontSize: isMobile ? 10 : 12 }} />
                <YAxis tick={{ fontSize: isMobile ? 10 : 12 }} />
                <Tooltip formatter={(value) => `${budgetData.currency} ${value}`} />
                {!isMobile && <Legend />}
                <Bar dataKey="cost" fill="#002B5B" name={t('components.budgetTab.dailySpending.chartLabel')} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-3 sm:mt-4 text-xs sm:text-sm text-muted-foreground">
            {t('components.budgetTab.dailySpending.average', { amount: `${budgetData.currency} ${(budgetData.total / dailyCosts.length).toFixed(2)}` })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
