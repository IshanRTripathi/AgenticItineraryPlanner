import { useEffect } from 'react';

interface ShortcutConfig {
  key: string;
  ctrl?: boolean;
  shift?: boolean;
  alt?: boolean;
  meta?: boolean;
}

export function useKeyboardShortcut(
  config: ShortcutConfig,
  callback: () => void,
  enabled: boolean = true
): void {
  useEffect(() => {
    if (!enabled) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      const matchesKey = event.key.toLowerCase() === config.key.toLowerCase();
      const matchesCtrl = config.ctrl ? event.ctrlKey : !event.ctrlKey;
      const matchesShift = config.shift ? event.shiftKey : !event.shiftKey;
      const matchesAlt = config.alt ? event.altKey : !event.altKey;
      const matchesMeta = config.meta ? event.metaKey : !event.metaKey;

      if (matchesKey && matchesCtrl && matchesShift && matchesAlt && matchesMeta) {
        event.preventDefault();
        callback();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [config, callback, enabled]);
}

export function useKeyboardShortcuts(
  shortcuts: Array<{ config: ShortcutConfig; callback: () => void; enabled?: boolean }>
): void {
  shortcuts.forEach(({ config, callback, enabled = true }) => {
    useKeyboardShortcut(config, callback, enabled);
  });
}
