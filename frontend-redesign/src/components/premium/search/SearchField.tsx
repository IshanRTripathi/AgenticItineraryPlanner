/**
 * SearchField Component
 * Individual search input field with icon and accessibility support
 */

import { ReactNode } from 'react';
import { motion } from 'framer-motion';

interface SearchFieldProps {
  icon: ReactNode;
  label: string;
  placeholder: string;
  value?: string;
  onChange?: (value: string) => void;
  isActive?: boolean;
  onFocus?: () => void;
  onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
  autoFocus?: boolean;
  readOnly?: boolean;
  onClick?: () => void;
}

export function SearchField({
  icon,
  label,
  placeholder,
  value,
  onChange,
  isActive,
  onFocus,
  onKeyDown,
  autoFocus,
  readOnly,
  onClick,
}: SearchFieldProps) {
  return (
    <motion.div
      className={`flex items-center gap-3 px-4 py-3 rounded-xl transition-all ${
        isActive
          ? 'bg-gray-50 ring-2 ring-primary-500'
          : 'hover:bg-gray-50'
      }`}
      whileHover={{ scale: 1.01 }}
      transition={{ duration: 0.15 }}
    >
      <div className="text-gray-400">{icon}</div>
      <div className="flex-1">
        <label htmlFor={label} className="sr-only">
          {label}
        </label>
        <input
          id={label}
          type="text"
          value={value}
          onChange={(e) => onChange?.(e.target.value)}
          onFocus={onFocus}
          onKeyDown={onKeyDown}
          onClick={onClick}
          placeholder={placeholder}
          autoFocus={autoFocus}
          readOnly={readOnly}
          aria-label={label}
          aria-required="true"
          className="w-full bg-transparent border-none outline-none text-sm font-medium text-gray-900 placeholder:text-gray-400"
        />
      </div>
    </motion.div>
  );
}
