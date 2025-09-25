import React, { useEffect } from 'react'
import { createPortal } from 'react-dom'

interface Props {
  open: boolean
  place: { name?: string; address?: string; lat: number; lng: number } | null
  days: Array<{ id: string; dayNumber: number; date?: string; location?: string }>
  onConfirm: (args: { dayId: string; dayNumber: number; place: { name: string; address?: string; lat: number; lng: number } }) => void
  onClose: () => void
}

export const PlaceAddModal: React.FC<Props> = ({ open, place, days, onConfirm, onClose }) => {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    if (open) document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open || !place) return null

  const container = document.body
  const title = place.name || 'Selected location'
  const subtitle = place.address || `${place.lat.toFixed(5)}, ${place.lng.toFixed(5)}`

  const onAdd = () => {
    const select = document.getElementById('place-day-select') as HTMLSelectElement
    const dayId = select?.value
    const d = days.find(x => x.id === dayId)
    if (!d) return
    onConfirm({ dayId: d.id, dayNumber: d.dayNumber, place: { name: title, address: place.address, lat: place.lat, lng: place.lng } })
  }

  return createPortal(
    <div style={{ position: 'fixed', inset: 0, zIndex: 50 }}>
      <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.4)' }} />
      <div style={{ position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', background: 'white', borderRadius: 12, width: 420, maxWidth: '90vw', boxShadow: '0 20px 25px -5px rgba(0,0,0,0.1), 0 8px 10px -6px rgba(0,0,0,0.1)' }}>
        <div style={{ padding: 16, borderBottom: '1px solid #e5e7eb', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <div style={{ fontWeight: 600 }}>Add place to itinerary</div>
          <button onClick={onClose} style={{ border: 'none', background: 'transparent', fontSize: 20, cursor: 'pointer' }}>×</button>
        </div>
        <div style={{ padding: 16 }}>
          <div style={{ marginBottom: 8 }}>
            <div style={{ fontWeight: 500 }}>{title}</div>
            <div style={{ color: '#6b7280' }}>{subtitle}</div>
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 8 }}>
            <label htmlFor="place-day-select" style={{ fontSize: 12, color: '#6b7280' }}>Day</label>
            <select id="place-day-select" style={{ flex: 1, border: '1px solid #e5e7eb', borderRadius: 6, padding: 6 }}>
              {days.map(d => (
                <option key={d.id} value={d.id}>{`Day ${d.dayNumber}${d.date ? ` • ${d.date}` : ''}`}</option>
              ))}
            </select>
            <button onClick={onAdd} style={{ background: '#2563eb', color: 'white', border: 'none', borderRadius: 6, padding: '6px 10px', cursor: 'pointer' }}>Add</button>
          </div>
        </div>
      </div>
    </div>,
    container
  )
}


