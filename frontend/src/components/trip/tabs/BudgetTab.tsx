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
import { useCurrency } from '@/hooks/useCurrency';
import { CurrencySelector } from '@/components/common/CurrencySelector';
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
  const { preferredCurrency, convert, formatCurrency } = useCurrency();
  const [metadata, setMetadata] = useState<any>(null);
  
  console.log('[BudgetTab] 🎯 Hook Values:', {
    preferredCurrency,
    hasConvert: !!convert,
    itineraryCurrency: itinerary?.currency
  });
  
  // Display currency: use preferred currency or itinerary currency
  const itineraryCurrency = itinerary?.currency || 'USD';
  const displayCurrency = useMemo(() => {
    const result = preferredCurrency || itineraryCurrency;
    console.log('[BudgetTab] 💱 Display Currency:', {
      preferredCurrency,
      itineraryCurrency,
      displayCurrency: result,
      usingPreferred: !!preferredCurrency
    });
    return result;
  }, [preferredCurrency, itineraryCurrency]);
  
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
  
  // Get AI-estimated budget from CityAllocationAgent
  const aiEstimatedBudget = useMemo(() => {
    if (!itinerary) {
      return null;
    }
    
    try {
      // Access agentData (exists in backend but not in TS interface yet)
      const agentData = (itinerary as any).agentData;
      if (!agentData?.cityAllocation) {
        return null;
      }
      
      const cityAllocationData = agentData.cityAllocation;
      const cityAllocation = cityAllocationData.data?.cityAllocation || cityAllocationData;
      
      if (cityAllocation?.budgetEstimate) {
        return {
          currency: cityAllocation.budgetEstimate.currency,
          minPerDay: cityAllocation.budgetEstimate.minPerPersonPerDay,
          maxPerDay: cityAllocation.budgetEstimate.maxPerPersonPerDay,
          rationale: cityAllocation.budgetEstimate.rationale,
          totalDays: itinerary.days?.length || 0,
        };
      }
    } catch (error) {
      console.error('Failed to parse AI budget estimate:', error);
    }
    
    return null;
  }, [itinerary]);
  
  // Calculate real budget data from itinerary (in original currency)
  const budgetData = useMemo(() => {
    if (!itinerary) {
      return { 
        total: 0, 
        spent: 0, 
        remaining: 0, 
        currency: 'USD',
        plannedBudget: 0,
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
    
    // IMPORTANT: Planned budget comes from AI estimate, not user input
    // User only provides tier (budget/medium/luxury), AI determines actual range
    const plannedBudget = aiEstimatedBudget 
      ? (aiEstimatedBudget.maxPerDay * aiEstimatedBudget.totalDays)
      : total;
    
    return {
      total,
      spent,
      remaining: total - spent,
      currency,
      plannedBudget,
    };
  }, [itinerary, aiEstimatedBudget]);
  
  // Convert budget data to display currency
  const convertedBudgetData = useMemo(() => {
    console.log('[BudgetTab] 🔄 Converting budget data:', {
      hasBudgetData: !!budgetData,
      budgetCurrency: budgetData?.currency,
      displayCurrency,
      needsConversion: budgetData && displayCurrency !== budgetData.currency,
      originalTotal: budgetData?.total
    });
    
    if (!budgetData || displayCurrency === budgetData.currency) {
      console.log('[BudgetTab] ⏭️ Skipping conversion (same currency or no data)');
      return budgetData;
    }
    
    const converted = {
      ...budgetData,
      total: convert(budgetData.total, budgetData.currency, displayCurrency),
      spent: convert(budgetData.spent, budgetData.currency, displayCurrency),
      remaining: convert(budgetData.remaining, budgetData.currency, displayCurrency),
      plannedBudget: convert(budgetData.plannedBudget, budgetData.currency, displayCurrency),
      currency: displayCurrency,
    };
    
    console.log('[BudgetTab] ✅ Conversion complete:', {
      originalTotal: budgetData.total,
      convertedTotal: converted.total,
      from: budgetData.currency,
      to: displayCurrency
    });
    
    return converted;
  }, [budgetData, displayCurrency, convert]);
  
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
  
  const percentageSpent = convertedBudgetData.plannedBudget > 0 ? (convertedBudgetData.spent / convertedBudgetData.plannedBudget) * 100 : 0;
  const isOverBudget = convertedBudgetData.spent > convertedBudgetData.plannedBudget;
  const isOverPlannedBudget = convertedBudgetData.total > convertedBudgetData.plannedBudget;

  return (
    <div className="space-y-3 sm:space-y-4 md:space-y-6">
      {/* Currency Selector - Top Right */}
      <div className="flex justify-end">
        <CurrencySelector variant="compact" showFlags={true} />
      </div>
      {/* AI Budget Rationale - Only show if there's a rationale */}
      {aiEstimatedBudget?.rationale && (
        <Card className="border-blue-200 bg-blue-50/50">
          <CardContent className="p-3 sm:p-4 md:p-6">
            <div className="flex items-start gap-2 sm:gap-3">
              <div className="flex-shrink-0 w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-blue-100 flex items-center justify-center">
                <DollarSign className="h-4 w-4 sm:h-5 sm:w-5 text-blue-600" />
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm sm:text-base font-semibold text-gray-900 mb-1">
                  Budget Insights for {itinerary?.days?.[0]?.location || 'Your Trip'}
                </h3>
                {userBudget.tier && (
                  <p className="text-xs text-gray-600 mb-2">
                    Based on your <span className="font-semibold capitalize">{userBudget.tier}</span> tier preference
                  </p>
                )}
                <p className="text-xs sm:text-sm text-gray-700 leading-relaxed">
                  {aiEstimatedBudget.rationale}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
      
      {/* Budget Overview - 3 Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-2 sm:gap-3 md:gap-4">
        {/* Card 1: AI Budget Range */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">
              Expected Budget Range
            </CardTitle>
            <DollarSign className="h-3 w-3 sm:h-4 sm:w-4 text-blue-600" />
          </CardHeader>
          <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
            {aiEstimatedBudget ? (
              <>
                <div className="text-base sm:text-lg md:text-xl font-bold text-blue-600 break-words">
                  {displayCurrency} {convert((aiEstimatedBudget.minPerDay * aiEstimatedBudget.totalDays), itineraryCurrency, displayCurrency).toLocaleString()}-
                  {convert((aiEstimatedBudget.maxPerDay * aiEstimatedBudget.totalDays), itineraryCurrency, displayCurrency).toLocaleString()}
                </div>
                <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
                  {displayCurrency} {convert(aiEstimatedBudget.minPerDay, itineraryCurrency, displayCurrency).toFixed(0)}-{convert(aiEstimatedBudget.maxPerDay, itineraryCurrency, displayCurrency).toFixed(0)} per day
                  {userBudget.tier && <span className="ml-1">• <span className="capitalize">{userBudget.tier}</span></span>}
                </p>
              </>
            ) : (
              <>
                <div className="text-base sm:text-lg md:text-xl font-bold break-words">
                  {convertedBudgetData.currency} {convertedBudgetData.total.toLocaleString()}
                </div>
                <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
                  Estimated total
                </p>
              </>
            )}
          </CardContent>
        </Card>
        
        {/* Card 2: Actual Calculated Cost */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
            <CardTitle className="text-xs sm:text-sm font-medium">Itinerary Cost</CardTitle>
            <DollarSign className="h-3 w-3 sm:h-4 sm:w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
            <div className={`text-base sm:text-lg md:text-xl font-bold break-words ${isOverPlannedBudget ? 'text-orange-600' : 'text-gray-900'}`}>
              {convertedBudgetData.currency} {convertedBudgetData.total.toLocaleString()}
            </div>
            <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
              {convertedBudgetData.total > 0 ? `${convertedBudgetData.currency} ${(convertedBudgetData.total / (itinerary?.days?.length || 1)).toFixed(0)} per day` : 'Calculating...'}
              {isOverPlannedBudget && <span className="text-orange-600 ml-1">• Over range</span>}
            </p>
          </CardContent>
        </Card>

        {/* Card 3: Booked Amount (only show if > 0) */}
        {convertedBudgetData.spent > 0 ? (
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
              <CardTitle className="text-xs sm:text-sm font-medium">Booked & Paid</CardTitle>
              <TrendingUp className="h-3 w-3 sm:h-4 sm:w-4 text-green-600" />
            </CardHeader>
            <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
              <div className="text-base sm:text-lg md:text-xl font-bold text-green-600 break-words">
                {convertedBudgetData.currency} {convertedBudgetData.spent.toLocaleString()}
              </div>
              <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
                {((convertedBudgetData.spent / convertedBudgetData.total) * 100).toFixed(0)}% of itinerary cost
              </p>
            </CardContent>
          </Card>
        ) : (
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 p-3 sm:p-4 md:p-6">
              <CardTitle className="text-xs sm:text-sm font-medium">Budget Status</CardTitle>
              <TrendingDown className="h-3 w-3 sm:h-4 sm:w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent className="p-3 sm:p-4 md:p-6 pt-0">
              <div className="text-base sm:text-lg md:text-xl font-bold text-gray-900 break-words">
                {isOverPlannedBudget ? (
                  <span className="text-orange-600">Over Budget</span>
                ) : (
                  <span className="text-green-600">Within Range</span>
                )}
              </div>
              <p className="text-xs text-muted-foreground mt-0.5 sm:mt-1">
                {isOverPlannedBudget 
                  ? `${convertedBudgetData.currency} ${(convertedBudgetData.total - convertedBudgetData.plannedBudget).toLocaleString()} over max`
                  : 'Ready to book'}
              </p>
            </CardContent>
          </Card>
        )}
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
                    <span className="text-xs sm:text-sm font-bold">{displayCurrency} {convert(category.value, itineraryCurrency, displayCurrency).toLocaleString()}</span>
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
                <Tooltip formatter={(value) => `${displayCurrency} ${convert(Number(value), itineraryCurrency, displayCurrency).toFixed(0)}`} />
                {!isMobile && <Legend />}
                <Bar dataKey="cost" fill="#002B5B" name={t('components.budgetTab.dailySpending.chartLabel')} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="mt-3 sm:mt-4 text-xs sm:text-sm text-muted-foreground">
            {t('components.budgetTab.dailySpending.average', { amount: `${displayCurrency} ${convert((budgetData.total / dailyCosts.length), itineraryCurrency, displayCurrency).toFixed(2)}` })}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
