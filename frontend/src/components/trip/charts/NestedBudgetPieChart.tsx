/**
 * Nested Pie Chart for Budget Breakdown
 * Inner circle: Category totals
 * Outer circle: Per-day breakdown of each category
 */

import { ResponsivePie } from '@nivo/pie';
import { useMemo } from 'react';

interface CategoryData {
  name: string;
  value: number;
  color: string;
}

interface DayBreakdown {
  day: number;
  categories: Record<string, number>;
}

interface NestedBudgetPieChartProps {
  categoryData: CategoryData[];
  days: DayBreakdown[];
  currency: string;
}

export function NestedBudgetPieChart({ categoryData, days, currency }: NestedBudgetPieChartProps) {
  // Prepare data for inner circle (categories)
  const innerData = useMemo(() => {
    return categoryData
      .filter(cat => cat.value > 0)
      .map(cat => ({
        id: cat.name,
        label: cat.name,
        value: cat.value,
        color: cat.color,
      }));
  }, [categoryData]);

  // Prepare data for outer circle (per-day breakdown)
  // Group by category first, then by day within each category
  const outerData = useMemo(() => {
    const result: any[] = [];
    
    // For each category, add all its days together
    categoryData.forEach(category => {
      if (category.value === 0) return;
      
      // Collect all days for this category
      days.forEach((day, dayIndex) => {
        const dayValue = day.categories[category.name] || 0;
        if (dayValue > 0) {
          // Create shaded version of category color based on day
          const opacity = 1 - (dayIndex * 0.15);
          result.push({
            id: `${category.name}-Day${day.day}`,
            label: `Day ${day.day}`,
            value: dayValue,
            color: category.color,
            opacity: Math.max(opacity, 0.4),
            category: category.name,
            sortOrder: categoryData.findIndex(c => c.name === category.name) * 100 + dayIndex,
          });
        }
      });
    });
    
    // Sort by category first, then by day
    return result.sort((a, b) => a.sortOrder - b.sortOrder);
  }, [categoryData, days]);

  const commonProps = {
    margin: { top: 40, right: 80, bottom: 80, left: 80 },
    sortByValue: false, // Disable sorting to maintain consistent order between inner and outer
    activeOuterRadiusOffset: 8,
    borderWidth: 1,
    borderColor: '#ffffff',
    enableArcLinkLabels: false,
    arcLabelsSkipAngle: 10,
    arcLabelsTextColor: '#ffffff',
    animate: true,
    motionConfig: 'gentle' as const,
  };

  return (
    <div className="relative w-full h-full">
      {/* Inner Circle - Categories (scaled down to 60%) */}
      <div 
        className="absolute inset-0 flex items-center justify-center" 
        style={{ zIndex: 1 }}
      >
        <div style={{ width: '60%', height: '60%' }}>
          <ResponsivePie
            {...commonProps}
            data={innerData}
            startAngle={0}
            endAngle={360}
            innerRadius={0}
            padAngle={2}
            cornerRadius={3}
            colors={{ datum: 'data.color' }}
            arcLabel={(d) => `${((d.value / innerData.reduce((sum, item) => sum + item.value, 0)) * 100).toFixed(0)}%`}
            arcLabelsTextColor="#ffffff"
            layers={['arcs', 'arcLabels']}
            theme={{
              tooltip: {
                container: {
                  background: 'hsl(var(--background))',
                  border: '1px solid hsl(var(--border))',
                  borderRadius: '0.5rem',
                  boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)',
                },
              },
            }}
            tooltip={({ datum }) => (
              <div className="bg-background border border-border rounded-lg shadow-lg px-3 py-2">
                <div className="flex items-center gap-2">
                  <div
                    className="w-3 h-3 rounded-full"
                    style={{ backgroundColor: datum.color }}
                  />
                  <span className="font-semibold text-sm">{datum.label}</span>
                </div>
                <div className="text-xs text-muted-foreground mt-1">
                  {currency} {datum.value.toLocaleString()}
                </div>
              </div>
            )}
          />
        </div>
      </div>

      {/* Outer Ring - Per Day (full size with inner radius) */}
      <div className="absolute inset-0" style={{ zIndex: 2 }}>
        <ResponsivePie
          {...commonProps}
          data={outerData}
          sortByValue={false}
          startAngle={0}
          endAngle={360}
          innerRadius={0.65}
          padAngle={1}
          cornerRadius={2}
          colors={(d) => d.data.color}
          arcLabel={() => ''}
          layers={['arcs']}
          theme={{
            tooltip: {
              container: {
                background: 'hsl(var(--background))',
                border: '1px solid hsl(var(--border))',
                borderRadius: '0.5rem',
                boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)',
              },
            },
          }}
          tooltip={({ datum }) => (
            <div className="bg-background border border-border rounded-lg shadow-lg px-3 py-2">
              <div className="flex items-center gap-2">
                <div
                  className="w-3 h-3 rounded-full"
                  style={{ 
                    backgroundColor: datum.data.color,
                    opacity: datum.data.opacity 
                  }}
                />
                <span className="font-semibold text-sm">{datum.data.category}</span>
              </div>
              <div className="text-xs text-muted-foreground">
                {datum.label}: {currency} {datum.value.toLocaleString()}
              </div>
            </div>
          )}
          defs={outerData.map((item, index) => ({
            id: `gradient-${index}`,
            type: 'linearGradient',
            colors: [
              { offset: 0, color: item.color, opacity: item.opacity },
              { offset: 100, color: item.color, opacity: item.opacity * 0.8 },
            ],
          }))}
          fill={outerData.map((item, index) => ({
            match: { id: item.id },
            id: `gradient-${index}`,
          }))}
        />
      </div>
    </div>
  );
}
