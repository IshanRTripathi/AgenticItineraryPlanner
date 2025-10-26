# EasyTrip Premium Redesign

Premium travel planning application with AI-powered itinerary generation.

## Design Philosophy

- **Apple.com refinement** - Clean, intuitive interfaces
- **Emirates.com luxury** - Premium feel and polish
- **EaseMyTrip functionality** - Complete travel booking

## Design Standards

- **Material 3** - Motion system and components
- **Apple HIG** - Touch targets (≥48px), glass morphism
- **Atlassian** - 12-column grid, 8px spacing

## Tech Stack

- React 18.3.1 + TypeScript
- Vite 6.3.5 (SWC)
- Tailwind CSS 3.4
- Framer Motion 11.x
- Lucide React (icons)

## Getting Started

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build
```

## Design Tokens

All design tokens are defined in `src/styles/tokens.css`:

- **Colors**: Deep Blue #002B5B (primary), Gold #F5C542 (secondary)
- **Spacing**: 8px base scale (8, 16, 24, 32, 40, 48, 64, 80, 96)
- **Typography**: Inter font, 12-60px scale
- **Shadows**: 3-layer elevation system
- **Motion**: 300ms cubic-bezier(0.4,0,0.2,1)
- **Radius**: Max 12px (no over-rounding)

## Component Library

Premium UI components in `src/components/ui/`:

- Button (primary, secondary, outline, ghost)
- Card (with header, content, footer)
- Input (standard + glass morphism)
- Badge (status indicators)
- Label (form labels)
- Avatar (user profiles)
- Skeleton (loading states)
- Separator (dividers)

## Project Structure

```
frontend-redesign/
├── src/
│   ├── components/
│   │   └── ui/           # UI primitives
│   ├── lib/
│   │   ├── animations.ts # Framer Motion configs
│   │   └── utils.ts      # Utilities
│   ├── styles/
│   │   └── tokens.css    # Design tokens
│   ├── App.tsx           # Main app
│   ├── main.tsx          # Entry point
│   └── index.css         # Global styles
├── index.html
├── package.json
├── tailwind.config.ts
├── tsconfig.json
└── vite.config.ts
```

## Development Guidelines

### Colors
- Use design tokens only (no arbitrary colors)
- Verify WCAG AA contrast (≥4.5:1 for normal text)

### Spacing
- Use 8px increments only (space-1 through space-12)
- No arbitrary spacing values

### Typography
- Use defined font sizes (text-xs through text-6xl)
- Use defined weights (300-800)

### Animations
- Target 60fps
- Use transform and opacity only (GPU accelerated)
- Use Material 3 easing: cubic-bezier(0.4,0,0.2,1)

### Components
- Max 12px border-radius
- Min 48px touch targets
- Use elevation system for shadows

## Browser Support

- Chrome (last 2 versions)
- Firefox (last 2 versions)
- Safari (last 2 versions)
- Edge (last 2 versions)

## License

Private - All rights reserved
