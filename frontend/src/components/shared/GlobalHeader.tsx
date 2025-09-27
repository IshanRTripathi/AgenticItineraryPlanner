import React from 'react';
import { Button } from '../ui/button';
import { ArrowLeft, Home } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { LanguageSelector } from './LanguageSelector';
import { UserProfileButton } from './UserProfileButton';
import { GlobalNavigation } from './GlobalNavigation';
import { BreadcrumbNavigation } from './BreadcrumbNavigation';

interface GlobalHeaderProps {
  title?: string;
  showBackButton?: boolean;
  onBack?: () => void;
  showHomeButton?: boolean;
  showNavigation?: boolean;
  showBreadcrumbs?: boolean;
  className?: string;
}

export function GlobalHeader({ 
  title, 
  showBackButton = false, 
  onBack,
  showHomeButton = false,
  showNavigation = false,
  showBreadcrumbs = false,
  className = ''
}: GlobalHeaderProps) {
  const navigate = useNavigate();

  const handleBack = () => {
    if (onBack) {
      onBack();
    } else {
      navigate(-1);
    }
  };

  const handleHome = () => {
    navigate('/');
  };

  return (
    <div className={`bg-white border-b border-gray-200 ${className}`}>
      {/* Main Header */}
      <div className="h-16 flex items-center justify-between px-6">
        <div className="flex items-center space-x-4">
          {showBackButton && (
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={handleBack}
              className="flex items-center space-x-2"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back</span>
            </Button>
          )}
          
          {showHomeButton && (
            <Button 
              variant="ghost" 
              size="sm" 
              onClick={handleHome}
              className="flex items-center space-x-2"
            >
              <Home className="w-4 h-4" />
              <span>Home</span>
            </Button>
          )}
          
          {title && (
            <h1 className="text-xl font-semibold">
              {title}
            </h1>
          )}
        </div>
        
        <div className="flex items-center space-x-3">
          {showNavigation && (
            <GlobalNavigation variant="horizontal" showLabels={false} />
          )}
          <LanguageSelector />
          <UserProfileButton />
        </div>
      </div>
      
      {/* Breadcrumbs */}
      {showBreadcrumbs && (
        <div className="px-6 py-2 border-t border-gray-100">
          <BreadcrumbNavigation />
        </div>
      )}
    </div>
  );
}
