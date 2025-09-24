import React from 'react'
import type { TerrainControlProps, MapTypeId } from '../../types/MapTypes'

export function TerrainControl({ currentMapType, onMapTypeChange, disabled }: TerrainControlProps) {
  const options: { value: MapTypeId; label: string }[] = [
    { value: 'roadmap', label: 'Roadmap' },
    { value: 'terrain', label: 'Terrain' },
    { value: 'satellite', label: 'Satellite' },
    { value: 'hybrid', label: 'Hybrid' },
  ]
  return (
    <div className="absolute top-3 right-3 bg-white shadow rounded p-2 flex gap-2">
      {options.map(opt => (
        <button
          key={opt.value}
          disabled={disabled}
          onClick={() => onMapTypeChange(opt.value)}
          className={`px-2 py-1 text-sm rounded ${currentMapType === opt.value ? 'bg-black text-white' : 'bg-gray-100'}`}
        >
          {opt.label}
        </button>
      ))}
    </div>
  )
}


