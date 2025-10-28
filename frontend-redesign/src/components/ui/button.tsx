/**
 * Button Component (Apple HIG-inspired)
 * Premium design with Material 3 motion
 */

import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';
import { cn } from '@/lib/utils';

const buttonVariants = cva(
  'inline-flex items-center justify-center rounded-md font-medium transition-all duration-normal ease-standard focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:opacity-50 disabled:pointer-events-none touch-target',
  {
    variants: {
      variant: {
        primary: 'bg-primary text-primary-foreground shadow-elevation-2 hover:bg-primary-hover hover:shadow-elevation-3 hover:-translate-y-0.5 active:scale-[0.98]',
        secondary: 'bg-secondary text-secondary-foreground shadow-elevation-2 hover:bg-secondary-hover hover:shadow-elevation-3 hover:-translate-y-0.5 active:scale-[0.98]',
        outline: 'border-2 border-primary text-primary hover:bg-primary/10 active:scale-[0.98]',
        ghost: 'hover:bg-muted hover:text-foreground active:scale-[0.98]',
        link: 'text-primary underline-offset-4 hover:underline',
      },
      size: {
        sm: 'h-11 md:h-10 px-3 text-sm',
        md: 'h-12 px-4 text-base',
        lg: 'h-14 px-6 text-lg',
        icon: 'h-11 w-11 md:h-10 md:w-10',
      },
    },
    defaultVariants: {
      variant: 'primary',
      size: 'md',
    },
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {
  asChild?: boolean;
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, size, ...props }, ref) => {
    return (
      <button
        className={cn(buttonVariants({ variant, size, className }))}
        ref={ref}
        {...props}
      />
    );
  }
);
Button.displayName = 'Button';

export { Button, buttonVariants };
