/**
 * UnifiedSearchBar Component
 * Premium search bar with animated placeholders and smooth interactions
 */

import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { MapPin, Calendar, Users, Search, ArrowLeftRight } from 'lucide-react';
import { SearchField } from './SearchField';
import { fadeInUp } from '@/lib/animations/variants';

interface SearchParams {
  from: string;
  to: string;
  dates?: string;
  travelers?: string;
}

interface UnifiedSearchBarProps {
  onSearch: (params: SearchParams) => void;
}

export function UnifiedSearchBar({ onSearch }: UnifiedSearchBarProps) {
  const [activeField, setActiveField] = useState<string | null>(null);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [dates, setDates] = useState('');
  const [travelers, setTravelers] = useState('');
  const [placeholderIndex, setPlaceholderIndex] = useState(0);

  const placeholders = ['Paris', 'Tokyo', 'New York', 'Barcelona', 'Dubai'];

  // Cycle through placeholder examples every 3 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      setPlaceholderIndex((prev) => (prev + 1) % placeholders.length);
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleSwap = () => {
    const temp = from;
    setFrom(to);
    setTo(temp);
  };

  const handleSearch = () => {
    onSearch({ from, to, dates, travelers });
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleSearch();
    }
  };

  return (
    <motion.div
      className="relative w-full max-w-5xl mx-auto"
      variants={fadeInUp}
      initial="initial"
      animate="animate"
    >
      <div className="flex items-center gap-2 p-2 bg-white rounded-2xl shadow-xl border border-gray-200">
        {/* From Field */}
        <SearchField
          icon={<MapPin className="w-5 h-5" />}
          label="From"
          placeholder={placeholders[placeholderIndex]}
          value={from}
          onChange={setFrom}
          isActive={activeField === 'from'}
          onFocus={() => setActiveField('from')}
          onKeyDown={handleKeyDown}
          autoFocus
        />

        {/* Swap Button */}
        <motion.button
          onClick={handleSwap}
          className="p-2 rounded-full hover:bg-gray-100 transition-colors"
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9, rotate: 180 }}
          transition={{ duration: 0.3 }}
          aria-label="Swap locations"
        >
          <ArrowLeftRight className="w-5 h-5 text-gray-600" />
        </motion.button>

        {/* To Field */}
        <SearchField
          icon={<MapPin className="w-5 h-5" />}
          label="To"
          placeholder="Where to?"
          value={to}
          onChange={setTo}
          isActive={activeField === 'to'}
          onFocus={() => setActiveField('to')}
          onKeyDown={handleKeyDown}
        />

        {/* Dates Field */}
        <SearchField
          icon={<Calendar className="w-5 h-5" />}
          label="Dates"
          placeholder="Add dates"
          value={dates}
          onChange={setDates}
          isActive={activeField === 'dates'}
          onFocus={() => setActiveField('dates')}
          onKeyDown={handleKeyDown}
        />

        {/* Travelers Field */}
        <SearchField
          icon={<Users className="w-5 h-5" />}
          label="Travelers"
          placeholder="Add guests"
          value={travelers}
          onChange={setTravelers}
          isActive={activeField === 'travelers'}
          onFocus={() => setActiveField('travelers')}
          onKeyDown={handleKeyDown}
        />

        {/* Search Button */}
        <motion.button
          onClick={handleSearch}
          className="flex items-center gap-2 px-6 py-3 bg-primary-500 text-white rounded-xl font-semibold whitespace-nowrap"
          whileHover={{
            scale: 1.05,
            boxShadow: '0 10px 20px rgba(14, 165, 233, 0.3)',
          }}
          whileTap={{ scale: 0.95 }}
          transition={{ duration: 0.2 }}
        >
          <Search className="w-5 h-5" />
          <span>Search</span>
        </motion.button>
      </div>

      {/* Microcopy */}
      <motion.p
        className="text-center text-sm text-gray-500 mt-3"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3 }}
      >
        Find your perfect trip in seconds
      </motion.p>
    </motion.div>
  );
}
