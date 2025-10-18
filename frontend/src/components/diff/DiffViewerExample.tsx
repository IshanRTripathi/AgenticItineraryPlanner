/**
 * Example usage of DiffViewer component
 * This can be used for testing and as a reference
 */

import React from 'react';
import { DiffViewer, DiffSection } from './DiffViewer';

export const DiffViewerExample: React.FC = () => {
  // Example diff data
  const exampleSections: DiffSection[] = [
    {
      title: 'Day 1 - Morning Activities',
      changes: [
        {
          type: 'added',
          path: 'day1.activity1',
          label: 'Sagrada Familia Visit',
          newValue: {
            time: '09:00 AM',
            duration: '2 hours',
            cost: '€26 per person',
            notes: 'Book tickets in advance',
          },
        },
        {
          type: 'modified',
          path: 'day1.activity2',
          label: 'Park Güell Visit',
          oldValue: {
            time: '11:00 AM',
            duration: '1.5 hours',
          },
          newValue: {
            time: '12:00 PM',
            duration: '2 hours',
          },
        },
      ],
    },
    {
      title: 'Day 1 - Afternoon & Evening',
      changes: [
        {
          type: 'removed',
          path: 'day1.activity3',
          label: 'Beach Time',
          oldValue: {
            time: '03:00 PM',
            duration: '2 hours',
            location: 'Barceloneta Beach',
          },
        },
        {
          type: 'added',
          path: 'day1.dinner',
          label: 'Dinner Reservation',
          newValue: {
            restaurant: 'Can Culleretes',
            time: '08:00 PM',
            cuisine: 'Traditional Catalan',
            cost: '€40-50 per person',
          },
        },
      ],
    },
    {
      title: 'Day 2 - Full Day',
      changes: [
        {
          type: 'modified',
          path: 'day2.accommodation',
          label: 'Hotel Booking',
          oldValue: {
            hotel: 'Hotel Arts Barcelona',
            checkIn: '2024-06-15',
            nights: 2,
            roomType: 'Standard Double',
          },
          newValue: {
            hotel: 'Hotel Arts Barcelona',
            checkIn: '2024-06-15',
            nights: 3,
            roomType: 'Deluxe Sea View',
          },
        },
        {
          type: 'added',
          path: 'day2.activity1',
          label: 'Montserrat Day Trip',
          newValue: {
            time: '08:00 AM',
            duration: 'Full day',
            transport: 'Train + Cable Car',
            cost: '€45 per person',
            highlights: ['Monastery', 'Mountain views', 'Hiking trails'],
          },
        },
      ],
    },
  ];

  return (
    <div style={{ height: '600px', padding: '20px' }}>
      <DiffViewer
        sections={exampleSections}
        viewMode="side-by-side"
        showUnchanged={false}
      />
    </div>
  );
};
