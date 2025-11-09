/**
 * Profile Page
 * Displays user information from authentication
 * Task 16: Mobile-optimized with single column layout and collapsible sections
 */

import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { 
  User, 
  Mail, 
  Calendar, 
  Shield, 
  ArrowLeft,
  CheckCircle2,
  Languages
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { LanguageSelector } from '@/i18n/components/LanguageSelector';
import { useTranslation } from '@/i18n/hooks/useTranslation';

export function ProfilePage() {
  const isMobile = useMediaQuery('(max-width: 768px)');
  const { user, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();

  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center">
            <p className="text-sm sm:text-base text-muted-foreground mb-4">{t('pages.profile.pleaseSignIn')}</p>
            <Button onClick={() => navigate('/login')} className="min-h-[48px] touch-manipulation">{t('common.actions.signIn')}</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Get user initials for avatar
  const getUserInitials = () => {
    if (user.displayName) {
      const names = user.displayName.split(' ');
      if (names.length >= 2) {
        return `${names[0].charAt(0)}${names[1].charAt(0)}`.toUpperCase();
      }
      return names[0].charAt(0).toUpperCase();
    }
    return user.email?.charAt(0).toUpperCase() || 'U';
  };

  // Format date
  const formatDate = (timestamp: string | undefined) => {
    if (!timestamp) return 'N/A';
    return new Date(timestamp).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="min-h-screen bg-gray-50 py-4 sm:py-6">
      <div className="container max-w-4xl px-3 sm:px-4">
        {/* Back Button */}
        <Button
          variant="ghost"
          onClick={() => navigate(-1)}
          className="mb-4 sm:mb-6 min-h-[44px] touch-manipulation active:scale-95 transition-transform"
        >
          <ArrowLeft className="w-4 h-4 mr-2" />
          {t('common.actions.back')}
        </Button>

        {/* Profile Header */}
        <Card className="mb-4 sm:mb-6">
          <CardContent className="pt-4 sm:pt-6 p-4 sm:p-6">
            <div className="flex flex-col sm:flex-row items-center sm:items-start gap-4 sm:gap-6">
              {/* Avatar */}
              <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-full bg-primary text-white flex items-center justify-center font-bold text-2xl sm:text-3xl shadow-lg flex-shrink-0">
                {getUserInitials()}
              </div>

              {/* User Info */}
              <div className="flex-1 text-center sm:text-left w-full">
                <div className="flex flex-col sm:flex-row items-center sm:items-center gap-2 sm:gap-3 mb-2">
                  <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">
                    {user.displayName || 'User'}
                  </h1>
                  {user.emailVerified && (
                    <Badge className="bg-green-100 text-green-700 border-green-200 text-xs">
                      <CheckCircle2 className="w-3 h-3 mr-1" />
                      Verified
                    </Badge>
                  )}
                </div>
                <p className="text-sm sm:text-base text-muted-foreground mb-3 sm:mb-4 break-all">{user.email}</p>
                <div className="flex flex-wrap justify-center sm:justify-start gap-2">
                  <Badge variant="outline" className="text-xs">Free Plan</Badge>
                  <Badge variant="outline" className="text-xs">
                    <span className="hidden sm:inline">Member since {new Date(user.metadata?.creationTime || '').getFullYear()}</span>
                    <span className="sm:hidden">Since {new Date(user.metadata?.creationTime || '').getFullYear()}</span>
                  </Badge>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Language Preferences */}
        <Card className="mb-4 sm:mb-6">
          <CardHeader className="p-4 sm:p-6">
            <CardTitle className="text-lg sm:text-xl flex items-center gap-2">
              <Languages className="w-5 h-5" />
              {t('pages.profile.languagePreferences')}
            </CardTitle>
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-indigo-100 flex items-center justify-center flex-shrink-0">
                <Languages className="w-4 h-4 sm:w-5 sm:h-5 text-indigo-600" />
              </div>
              <div className="flex-1">
                <p className="text-xs sm:text-sm font-medium text-gray-900 mb-3">
                  {t('pages.profile.selectLanguage')}
                </p>
                <LanguageSelector variant="dropdown" />
                <p className="text-xs text-muted-foreground mt-2">
                  {t('pages.profile.languageDescription')}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Account Details */}
        <Card className="mb-4 sm:mb-6">
          <CardHeader className="p-4 sm:p-6">
            <CardTitle className="text-lg sm:text-xl">{t('pages.profile.accountDetails')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 sm:space-y-4 p-4 sm:p-6">
            {/* Email */}
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg touch-manipulation">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
                <Mail className="w-4 h-4 sm:w-5 sm:h-5 text-blue-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-900 mb-1">Email Address</p>
                <p className="text-xs sm:text-sm text-muted-foreground break-all">{user.email}</p>
                {user.emailVerified ? (
                  <Badge className="mt-2 bg-green-100 text-green-700 border-green-200 text-xs">
                    Verified
                  </Badge>
                ) : (
                  <Badge variant="outline" className="mt-2 text-xs">
                    Not Verified
                  </Badge>
                )}
              </div>
            </div>

            {/* Display Name */}
            {user.displayName && (
              <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg touch-manipulation">
                <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-purple-100 flex items-center justify-center flex-shrink-0">
                  <User className="w-4 h-4 sm:w-5 sm:h-5 text-purple-600" />
                </div>
                <div className="flex-1">
                  <p className="text-xs sm:text-sm font-medium text-gray-900 mb-1">Display Name</p>
                  <p className="text-xs sm:text-sm text-muted-foreground">{user.displayName}</p>
                </div>
              </div>
            )}

            {/* User ID */}
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg touch-manipulation">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-gray-200 flex items-center justify-center flex-shrink-0">
                <Shield className="w-4 h-4 sm:w-5 sm:h-5 text-gray-600" />
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs sm:text-sm font-medium text-gray-900 mb-1">User ID</p>
                <p className="text-xs text-muted-foreground font-mono break-all">{user.uid}</p>
              </div>
            </div>

            {/* Account Created */}
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg touch-manipulation">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-green-100 flex items-center justify-center flex-shrink-0">
                <Calendar className="w-4 h-4 sm:w-5 sm:h-5 text-green-600" />
              </div>
              <div className="flex-1">
                <p className="text-xs sm:text-sm font-medium text-gray-900 mb-1">Member Since</p>
                <p className="text-xs sm:text-sm text-muted-foreground">
                  {formatDate(user.metadata?.creationTime)}
                </p>
              </div>
            </div>

            {/* Last Sign In */}
            <div className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg touch-manipulation">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-amber-100 flex items-center justify-center flex-shrink-0">
                <Calendar className="w-4 h-4 sm:w-5 sm:h-5 text-amber-600" />
              </div>
              <div className="flex-1">
                <p className="text-xs sm:text-sm font-medium text-gray-900 mb-1">Last Sign In</p>
                <p className="text-xs sm:text-sm text-muted-foreground">
                  {formatDate(user.metadata?.lastSignInTime)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Provider Info */}
        <Card>
          <CardHeader className="p-4 sm:p-6">
            <CardTitle className="text-lg sm:text-xl">{t('pages.profile.authProvider')}</CardTitle>
          </CardHeader>
          <CardContent className="p-4 sm:p-6">
            <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg touch-manipulation">
              <div className="w-9 h-9 sm:w-10 sm:h-10 rounded-full bg-white border-2 border-gray-200 flex items-center justify-center flex-shrink-0">
                <svg className="w-4 h-4 sm:w-5 sm:h-5" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
              </div>
              <div>
                <p className="text-xs sm:text-sm font-medium text-gray-900">Google</p>
                <p className="text-xs text-muted-foreground">Signed in with Google account</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
