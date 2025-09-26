import React, { useState } from 'react'

interface DayOption { id: string; dayNumber: number; date?: string; location?: string }

interface Props {
  place: { 
    name?: string; 
    address?: string; 
    lat: number; 
    lng: number;
    types?: string[];
    rating?: number;
    userRatingCount?: number;
    phoneNumber?: string;
    mapsLink?: string;
  }
  days: DayOption[]
  onAdd: (args: { dayId: string; dayNumber: number }) => void
  onClose: () => void
}

export const PlaceInfoCard: React.FC<Props> = ({ place, days, onAdd, onClose }) => {
  const [selectedDayId, setSelectedDayId] = useState(days[0]?.id || '')
  const title = place.name || 'Selected location'
  const subtitle = place.address || `${place.lat.toFixed(5)}, ${place.lng.toFixed(5)}`

  const handleAdd = () => {
    const selectedDay = days.find(d => d.id === selectedDayId)
    if (!selectedDay) return
    onAdd({ dayId: selectedDay.id, dayNumber: selectedDay.dayNumber })
  }

  return (
    <div style={{ 
      width: '100%',
      maxWidth: 400,
      minWidth: 320,
      padding: 12, 
      fontFamily: 'system-ui, -apple-system, sans-serif',
      fontSize: 14,
      lineHeight: 1.4,
      boxSizing: 'border-box'
    }}>
      {/* Header */}
      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: 8,
        borderBottom: '1px solid #e5e7eb',
        paddingBottom: 6
      }}>
        <h2 style={{ 
          margin: 0, 
          fontWeight: 700, 
          fontSize: 18, 
          color: '#111827',
          letterSpacing: '-0.025em'
        }}>
          Add to itinerary
        </h2>
        {/* Remove inline close to avoid duplication with InfoWindow close button */}
      </div>

      {/* Place Info */}
      <div style={{ marginBottom: 10 }}>
        <div style={{ 
          fontWeight: 600, 
          fontSize: 15, 
          color: '#1f2937',
          marginBottom: 3,
          wordBreak: 'break-word'
        }}>
          {title}
        </div>
        <div style={{ 
          color: '#6b7280', 
          fontSize: 13,
          wordBreak: 'break-word'
        }}>
          {subtitle}
        </div>
      </div>

      {/* Day Selection */}
      <div style={{ marginBottom: 10 }}>
        <label 
          htmlFor="place-day-select" 
          style={{ 
            display: 'block',
            fontSize: 12, 
            color: '#6b7280',
            marginBottom: 4,
            fontWeight: 500
          }}
        >
          Select Day
        </label>
        <select 
          id="place-day-select"
          value={selectedDayId}
          onChange={(e) => setSelectedDayId(e.target.value)}
          style={{ 
            width: '100%',
            border: '1px solid #d1d5db', 
            borderRadius: 6, 
            padding: '6px 10px',
            fontSize: 14,
            backgroundColor: 'white',
            color: '#1f2937'
          }}
        >
          {days.map(d => (
            <option key={d.id} value={d.id}>
              Day {d.dayNumber}{d.date ? ` • ${d.date}` : ''}
            </option>
          ))}
        </select>
      </div>

      {/* Actions */}
      <div style={{ 
        display: 'flex', 
        gap: 8, 
        justifyContent: 'flex-end',
        flexWrap: 'wrap'
      }}>
        <button
          onClick={onClose}
          style={{ 
            background: 'transparent', 
            color: '#6b7280', 
            border: '1px solid #d1d5db', 
            borderRadius: 6, 
            padding: '8px 16px', 
            cursor: 'pointer',
            fontSize: 13,
            fontWeight: 500,
            minWidth: 80,
            flex: '1 1 auto'
          }}
        >
          Cancel
        </button>
        <button
          onClick={handleAdd}
          style={{ 
            background: '#2563eb', 
            color: 'white', 
            border: 'none', 
            borderRadius: 6, 
            padding: '8px 16px', 
            cursor: 'pointer',
            fontSize: 13,
            fontWeight: 500,
            minWidth: 100,
            flex: '1 1 auto'
          }}
        >
          Add to Day
        </button>
      </div>
    </div>
  )
}


