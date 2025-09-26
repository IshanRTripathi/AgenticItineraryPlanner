/**
 * User Profile Component
 * Displays user information and provides profile management functionality
 */

import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Button } from './ui/button';
import { Card } from './ui/card';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { 
  User, 
  Mail, 
  LogOut, 
  Settings, 
  Edit3,
  Check,
  X
} from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface UserProfileProps {
  className?: string;
  showLogout?: boolean;
  compact?: boolean;
}

export const UserProfile: React.FC<UserProfileProps> = ({
  className = '',
  showLogout = true,
  compact = false
}) => {
  const { user, signOut } = useAuth();
  const { t } = useTranslation();
  const [isEditing, setIsEditing] = useState(false);

  if (!user) {
    return null;
  }

  const handleSignOut = async () => {
    try {
      await signOut();
      console.log('[UserProfile] User signed out successfully');
    } catch (error) {
      console.error('[UserProfile] Sign out failed:', error);
    }
  };

  // Removed password reset functionality - using Google Sign-in only

  const getInitials = (name: string | null): string => {
    if (!name) return 'U';
    return name
      .split(' ')
      .map(word => word.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  if (compact) {
    return (
      <div className={`flex items-center space-x-3 ${className}`}>
        <Avatar className="h-8 w-8">
          <AvatarImage src={user.photoURL || undefined} alt={user.displayName || 'User'} />
          <AvatarFallback className="text-xs">
            {getInitials(user.displayName)}
          </AvatarFallback>
        </Avatar>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-gray-900 truncate">
            {user.displayName || 'User'}
          </p>
          <p className="text-xs text-gray-500 truncate">
            {user.email}
          </p>
        </div>
        {showLogout && (
          <Button
            variant="ghost"
            size="sm"
            onClick={handleSignOut}
            className="text-gray-500 hover:text-gray-700"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        )}
      </div>
    );
  }

  return (
    <Card className={`p-6 ${className}`}>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-900">Profile</h2>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setIsEditing(!isEditing)}
        >
          {isEditing ? (
            <>
              <X className="h-4 w-4 mr-2" />
              Cancel
            </>
          ) : (
            <>
              <Edit3 className="h-4 w-4 mr-2" />
              Edit
            </>
          )}
        </Button>
      </div>

      <div className="space-y-6">
        {/* User Avatar and Basic Info */}
        <div className="flex items-center space-x-4">
          <Avatar className="h-16 w-16">
            <AvatarImage src={user.photoURL || undefined} alt={user.displayName || 'User'} />
            <AvatarFallback className="text-lg">
              {getInitials(user.displayName)}
            </AvatarFallback>
          </Avatar>
          <div>
            <h3 className="text-lg font-medium text-gray-900">
              {user.displayName || 'User'}
            </h3>
            <p className="text-sm text-gray-500">{user.email}</p>
            <div className="flex items-center mt-1">
              <div className={`w-2 h-2 rounded-full mr-2 ${
                user.emailVerified ? 'bg-green-500' : 'bg-yellow-500'
              }`} />
              <span className="text-xs text-gray-500">
                {user.emailVerified ? 'Email verified' : 'Email not verified'}
              </span>
            </div>
          </div>
        </div>

        {/* User Details */}
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <Label htmlFor="displayName" className="text-sm font-medium text-gray-700">
                Display Name
              </Label>
              <div className="mt-1 flex items-center space-x-2">
                <User className="h-4 w-4 text-gray-400" />
                <span className="text-sm text-gray-900">
                  {user.displayName || 'Not set'}
                </span>
              </div>
            </div>

            <div>
              <Label htmlFor="email" className="text-sm font-medium text-gray-700">
                Email Address
              </Label>
              <div className="mt-1 flex items-center space-x-2">
                <Mail className="h-4 w-4 text-gray-400" />
                <span className="text-sm text-gray-900">{user.email}</span>
              </div>
            </div>
          </div>

          <div>
            <Label htmlFor="userId" className="text-sm font-medium text-gray-700">
              User ID
            </Label>
            <div className="mt-1">
              <code className="text-xs bg-gray-100 px-2 py-1 rounded text-gray-700">
                {user.uid}
              </code>
            </div>
          </div>
        </div>

        {/* Security Section - Google Sign-in only */}
        <div className="border-t pt-4">
          <h4 className="text-sm font-medium text-gray-900 mb-3">Security</h4>
          <div className="space-y-3">
            <div className="text-sm text-gray-600">
              <p>Signed in with Google</p>
              <p className="text-xs text-gray-500 mt-1">
                To change your password or security settings, please visit your Google Account.
              </p>
            </div>
          </div>
        </div>

        {/* Actions */}
        {showLogout && (
          <div className="border-t pt-4">
            <Button
              variant="outline"
              onClick={handleSignOut}
              className="w-full"
            >
              <LogOut className="h-4 w-4 mr-2" />
              Sign Out
            </Button>
          </div>
        )}
      </div>
    </Card>
  );
};

export default UserProfile;
