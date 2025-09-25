import React from 'react'

interface DayOption { id: string; dayNumber: number; date?: string; location?: string }

interface Props {
  place: { name?: string; address?: string; lat: number; lng: number }
  days: DayOption[]
  onAdd: (args: { dayId: string; dayNumber: number }) => void
  onClose: () => void
}

export const PlaceInfoCard: React.FC<Props> = ({ place, days, onAdd, onClose }) => {
  const title = place.name || 'Selected location'
  const subtitle = place.address || `${place.lat.toFixed(5)}, ${place.lng.toFixed(5)}`

  return (
    <div style={{ width: 380 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <div style={{ fontWeight: 600 }}>Add to itinerary</div>
        <button onClick={onClose} style={{ border: 'none', background: 'transparent', fontSize: 16, cursor: 'pointer' }}>×</button>
      </div>
      <div style={{ marginBottom: 8 }}>
        <div style={{ fontWeight: 500 }}>{title}</div>
        <div style={{ color: '#6b7280' }}>{subtitle}</div>
      </div>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <label htmlFor="place-day-select" style={{ fontSize: 12, color: '#6b7280' }}>Day</label>
        <select id="place-day-select" style={{ flex: 1, border: '1px solid #e5e7eb', borderRadius: 6, padding: 6 }}>
          {days.map(d => (
            <option key={d.id} value={d.id}>{`Day ${d.dayNumber}${d.date ? ` • ${d.date}` : ''}`}</option>
          ))}
        </select>
        <button
          onClick={() => {
            const el = document.getElementById('place-day-select') as HTMLSelectElement
            const d = days.find(x => x.id === el?.value)
            if (!d) return
            onAdd({ dayId: d.id, dayNumber: d.dayNumber })
          }}
          style={{ background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, padding: '6px 10px', cursor: 'pointer' }}
        >
          Add
        </button>
      </div>
    </div>
  )
}


