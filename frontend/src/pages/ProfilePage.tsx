/**
 * Profile Page - Enhanced UI/UX
 * Modern profile page with improved visual design and user experience
 * Features: Gradient header, stats cards, quick actions, smooth animations
 */

import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { BottomSheet } from '@/components/ui/bottom-sheet';
import { 
  User, 
  Mail, 
  Calendar, 
  ArrowLeft,
  Languages,
  CreditCard,
  MapPin,
  Plane,
  Edit,
  LogOut,
  Award,
  TrendingUp
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { LanguageSelector } from '@/i18n/components/LanguageSelector';
import { useTranslation } from '@/i18n/hooks/useTranslation';
import { motion } from 'framer-motion';

export function ProfilePage() {
  const { user, isAuthenticated, signOut } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();
  const [languageSheetOpen, setLanguageSheetOpen] = useState(false);

  if (!isAuthenticated || !user) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ duration: 0.3 }}
        >
          <Card className="w-full max-w-md shadow-xl">
            <CardContent className="pt-6 text-center">
              <div className="w-16 h-16 mx-auto mb-4 rounded-full bg-primary/10 flex items-center justify-center">
                <User className="w-8 h-8 text-primary" />
              </div>
              <p className="text-sm sm:text-base text-muted-foreground mb-4">{t('pages.profile.pleaseSignIn')}</p>
              <Button onClick={() => navigate('/login')} className="min-h-[48px] touch-manipulation">
                {t('common.actions.signIn')}
              </Button>
            </CardContent>
          </Card>
        </motion.div>
      </div>
    );
  }

  const handleSignOut = async () => {
    await signOut();
    navigate('/');
  };

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
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-blue-50/30 to-gray-50">
      {/* Gradient Header with Profile */}
      <div className="relative bg-gradient-to-r from-primary via-primary-hover to-primary overflow-hidden">
        {/* Decorative background pattern */}
        <div className="absolute inset-0 opacity-10">
          <div className="absolute top-0 left-0 w-64 h-64 bg-white rounded-full -translate-x-1/2 -translate-y-1/2" />
          <div className="absolute bottom-0 right-0 w-96 h-96 bg-white rounded-full translate-x-1/3 translate-y-1/3" />
        </div>

        <div className="container max-w-5xl px-3 sm:px-6 relative">
          {/* Back Button - Properly positioned at top */}
          {/* Profile Header - Centered */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="flex flex-col items-center text-center py-6 sm:py-8"
          >
            {/* Avatar - Clean without tick mark */}
            <div className="relative group mb-4">
              <div className="w-24 h-24 sm:w-32 sm:h-32 rounded-2xl bg-white text-primary flex items-center justify-center font-bold text-4xl sm:text-5xl shadow-2xl ring-4 ring-white/20 transition-transform group-hover:scale-105">
                {getUserInitials()}
              </div>
            </div>

            {/* User Info */}
            <div className="max-w-md">
              <h1 className="text-2xl sm:text-4xl font-bold text-white drop-shadow-lg mb-2">
                {user.displayName || 'User'}
              </h1>
              <p className="text-white/90 text-sm sm:text-base mb-6 flex items-center justify-center gap-2">
                <Mail className="w-4 h-4" />
                <span className="truncate max-w-[280px]">{user.email}</span>
              </p>
              <div className="flex flex-wrap justify-center gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  className="bg-white/20 hover:bg-white/30 text-white border-white/30 backdrop-blur-sm text-xs sm:text-sm h-9 min-h-[36px]"
                >
                  <Edit className="w-4 h-4 mr-2" />
                  {t('pages.profile.editProfile')}
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={handleSignOut}
                  className="bg-white/20 hover:bg-white/30 text-white border-white/30 backdrop-blur-sm text-xs sm:text-sm h-9 min-h-[36px]"
                >
                  <LogOut className="w-4 h-4 mr-2" />
                  {t('common.actions.signOut')}
                </Button>
              </div>
            </div>
          </motion.div>
        </div>
      </div>

      <div className="container max-w-5xl px-3 sm:px-6 py-6 sm:py-8 relative z-10">
        {/* Stats Cards - Compact */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.1 }}
          className="grid grid-cols-3 gap-2 sm:gap-4 mb-4 sm:mb-6 -mt-8 sm:-mt-12"
        >
          <Card className="bg-white shadow-lg hover:shadow-xl transition-shadow">
            <CardContent className="p-2 sm:p-4">
              <div className="flex flex-col items-center gap-1 sm:gap-2">
                <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-primary/10 flex items-center justify-center">
                  <Plane className="w-4 h-4 sm:w-5 sm:h-5 text-primary" />
                </div>
                <div className="text-center">
                  <p className="text-xl sm:text-2xl font-bold text-primary">0</p>
                  <p className="text-[10px] sm:text-xs text-muted-foreground">{t('pages.profile.stats.totalTrips')}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-white shadow-lg hover:shadow-xl transition-shadow">
            <CardContent className="p-2 sm:p-4">
              <div className="flex flex-col items-center gap-1 sm:gap-2">
                <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-green-100 flex items-center justify-center">
                  <MapPin className="w-4 h-4 sm:w-5 sm:h-5 text-green-600" />
                </div>
                <div className="text-center">
                  <p className="text-xl sm:text-2xl font-bold text-green-600">0</p>
                  <p className="text-[10px] sm:text-xs text-muted-foreground">{t('pages.profile.stats.places')}</p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-white shadow-lg hover:shadow-xl transition-shadow">
            <CardContent className="p-2 sm:p-4">
              <div className="flex flex-col items-center gap-1 sm:gap-2">
                <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-full bg-amber-100 flex items-center justify-center">
                  <Award className="w-4 h-4 sm:w-5 sm:h-5 text-amber-600" />
                </div>
                <div className="text-center">
                  <p className="text-lg sm:text-xl font-bold text-amber-600">
                    {new Date(user.metadata?.creationTime || '').getFullYear()}
                  </p>
                  <p className="text-[10px] sm:text-xs text-muted-foreground">{t('pages.profile.stats.since')}</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </motion.div>

        {/* Main Content - Single Column */}
        <div className="space-y-4 sm:space-y-6">
          {/* Quick Actions - Compact */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            <Card className="shadow-lg">
              <CardHeader className="p-3 sm:p-4">
                <CardTitle className="text-sm sm:text-base flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-primary" />
                  {t('pages.profile.quickActions.title')}
                </CardTitle>
                <CardDescription className="text-xs">{t('pages.profile.quickActions.description')}</CardDescription>
              </CardHeader>
              <CardContent className="p-3 sm:p-4 pt-0 grid grid-cols-2 gap-2">
                <Button
                  variant="outline"
                  className="flex-col h-auto py-3 hover:bg-primary/5 hover:border-primary transition-all group"
                  onClick={() => navigate('/trips')}
                >
                  <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center group-hover:bg-primary/20 transition-colors mb-2">
                    <Plane className="w-4 h-4 text-primary" />
                  </div>
                  <p className="font-semibold text-xs">{t('pages.profile.quickActions.myTrips')}</p>
                </Button>

                <Button
                  variant="outline"
                  className="flex-col h-auto py-3 hover:bg-green-50 hover:border-green-500 transition-all group"
                  onClick={() => navigate('/planner')}
                >
                  <div className="w-8 h-8 rounded-lg bg-green-100 flex items-center justify-center group-hover:bg-green-200 transition-colors mb-2">
                    <MapPin className="w-4 h-4 text-green-600" />
                  </div>
                  <p className="font-semibold text-xs">{t('pages.profile.quickActions.planTrip')}</p>
                </Button>

                <Button
                  variant="outline"
                  className="flex-col h-auto py-3 hover:bg-amber-50 hover:border-amber-500 transition-all group"
                >
                  <div className="w-8 h-8 rounded-lg bg-amber-100 flex items-center justify-center group-hover:bg-amber-200 transition-colors mb-2">
                    <CreditCard className="w-4 h-4 text-amber-600" />
                  </div>
                  <p className="font-semibold text-xs">{t('pages.profile.quickActions.upgrade')}</p>
                </Button>

                <Button
                  variant="outline"
                  className="flex-col h-auto py-3 hover:bg-indigo-50 hover:border-indigo-500 transition-all group"
                  onClick={() => setLanguageSheetOpen(true)}
                >
                  <div className="w-8 h-8 rounded-lg bg-indigo-100 flex items-center justify-center group-hover:bg-indigo-200 transition-colors mb-2">
                    <Languages className="w-4 h-4 text-indigo-600" />
                  </div>
                  <p className="font-semibold text-xs">{t('pages.profile.quickActions.language')}</p>
                </Button>
              </CardContent>
            </Card>
          </motion.div>

          {/* Account Information - Compact */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            <Card className="shadow-lg">
              <CardHeader className="p-3 sm:p-4">
                <CardTitle className="text-sm sm:text-base flex items-center gap-2">
                  <User className="w-4 h-4 text-primary" />
                  {t('pages.profile.accountDetails')}
                </CardTitle>
                <CardDescription className="text-xs">{t('pages.profile.accountDetailsDescription')}</CardDescription>
              </CardHeader>
              <CardContent className="p-3 sm:p-4 pt-0">
                {/* Account Created & Last Sign In - Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                  <div className="flex items-center gap-2 p-2 sm:p-3 bg-gradient-to-r from-green-50 to-transparent rounded-lg border border-green-100">
                    <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
                      <Calendar className="w-4 h-4 sm:w-5 sm:h-5 text-green-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-semibold text-gray-900">{t('pages.profile.memberSince')}</p>
                      <p className="text-xs text-muted-foreground truncate">
                        {formatDate(user.metadata?.creationTime)}
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 p-2 sm:p-3 bg-gradient-to-r from-amber-50 to-transparent rounded-lg border border-amber-100">
                    <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-lg bg-amber-100 flex items-center justify-center flex-shrink-0">
                      <Calendar className="w-4 h-4 sm:w-5 sm:h-5 text-amber-600" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-semibold text-gray-900">{t('pages.profile.lastSignIn')}</p>
                      <p className="text-xs text-muted-foreground truncate">
                        {formatDate(user.metadata?.lastSignInTime)}
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </motion.div>
        </div>
      </div>

      {/* Language Selection Bottom Sheet */}
      <BottomSheet
        open={languageSheetOpen}
        onOpenChange={setLanguageSheetOpen}
        title={t('pages.profile.languagePreferences')}
      >
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            {t('pages.profile.languageDescription')}
          </p>
          <LanguageSelector variant="inline" showFlags={true} />
        </div>
      </BottomSheet>
    </div>
  );
}
