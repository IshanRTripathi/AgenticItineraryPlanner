import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Home, Search, PlusCircle, Calendar, User } from 'lucide-react';

type IconComponentType = React.ElementType<{ className?: string }>;

export interface InteractiveMenuItem {
  label: string;
  icon: IconComponentType;
  path?: string;
  isCenter?: boolean;
}

export interface InteractiveMenuProps {
  items?: InteractiveMenuItem[];
  accentColor?: string;
  activeIndex?: number;
  onItemClick?: (index: number, path?: string) => void;
  isHidden?: boolean;
}

const defaultItems: InteractiveMenuItem[] = [
  { label: 'Home', icon: Home, path: '/' },
  { label: 'Search', icon: Search, path: '/search' },
  { label: 'Plan', icon: PlusCircle, path: '/planner', isCenter: true },
  { label: 'Trips', icon: Calendar, path: '/dashboard' },
  { label: 'Profile', icon: User, path: '/profile' },
];

const defaultAccentColor = 'hsl(var(--primary))';

const InteractiveMenu: React.FC<InteractiveMenuProps> = ({ 
  items, 
  accentColor,
  activeIndex: controlledActiveIndex,
  onItemClick,
  isHidden = false
}) => {
  const finalItems = useMemo(() => {
    const isValid = items && Array.isArray(items) && items.length >= 2 && items.length <= 5;
    if (!isValid) {
      console.warn("InteractiveMenu: 'items' prop is invalid or missing. Using default items.", items);
      return defaultItems;
    }
    return items;
  }, [items]);

  const [internalActiveIndex, setInternalActiveIndex] = useState(0);
  const activeIndex = controlledActiveIndex !== undefined ? controlledActiveIndex : internalActiveIndex;

  useEffect(() => {
    if (activeIndex >= finalItems.length) {
      setInternalActiveIndex(0);
    }
  }, [finalItems, activeIndex]);

  const textRefs = useRef<(HTMLElement | null)[]>([]);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);

  useEffect(() => {
    const setLineWidth = () => {
      const activeItemElement = itemRefs.current[activeIndex];
      const activeTextElement = textRefs.current[activeIndex];
      if (activeItemElement && activeTextElement) {
        const textWidth = activeTextElement.offsetWidth;
        activeItemElement.style.setProperty('--lineWidth', `${textWidth}px`);
      }
    };

    setLineWidth();
    window.addEventListener('resize', setLineWidth);
    return () => {
      window.removeEventListener('resize', setLineWidth);
    };
  }, [activeIndex, finalItems]);

  const handleItemClick = (index: number) => {
    if (controlledActiveIndex === undefined) {
      setInternalActiveIndex(index);
    }
    if (onItemClick) {
      onItemClick(index, finalItems[index].path);
    }
  };

  const navStyle = useMemo(() => {
    const activeColor = accentColor || defaultAccentColor;
    return { '--component-active-color': activeColor } as React.CSSProperties;
  }, [accentColor]);

  // Find center button
  const centerIndex = finalItems.findIndex(item => item.isCenter);
  const centerItem = centerIndex >= 0 ? finalItems[centerIndex] : null;

  return (
    <div className={`menu ${isHidden ? 'hidden' : ''}`}>
      {/* Center floating button */}
      {centerItem && (
        <button
          className="menu__item center"
          onClick={() => handleItemClick(centerIndex)}
          aria-label={centerItem.label}
        >
          <div className="menu__icon">
            <centerItem.icon className="icon" />
          </div>
        </button>
      )}

      {/* Menu bar with other items */}
      <nav
        className="menu__bar"
        role="navigation"
        style={navStyle}
      >
        {finalItems.map((item, index) => {
          const isActive = index === activeIndex;
          const isTextActive = isActive && !item.isCenter;
          const IconComponent = item.icon;
          const isCenter = item.isCenter;

          // Skip center item (rendered above)
          if (isCenter) {
            return <div key={item.label} className="menu__spacer" />;
          }

          return (
            <button
              key={item.label}
              className={`menu__item ${isActive ? 'active' : ''}`}
              onClick={() => handleItemClick(index)}
              ref={(el) => (itemRefs.current[index] = el)}
              style={{ '--lineWidth': '0px' } as React.CSSProperties}
              aria-label={item.label}
            >
              <div className="menu__icon">
                <IconComponent className="icon" />
              </div>
              <strong
                className={`menu__text ${isTextActive ? 'active' : ''}`}
                ref={(el) => (textRefs.current[index] = el)}
              >
                {item.label}
              </strong>
            </button>
          );
        })}
      </nav>
    </div>
  );
};

export { InteractiveMenu };
