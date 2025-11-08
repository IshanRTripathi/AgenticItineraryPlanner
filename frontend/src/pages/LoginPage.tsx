/**
 * Login Page
 * Google Sign-In with feature showcase
 * Mobile: Glassmorphism design with sliding carousel
 * Desktop: Split screen with features
 */

import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { Plane, Sparkles, Shield, Calendar, CheckCircle2 } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/utils';

const FEATURES = [
  {
    icon: Sparkles,
    title: 'AI-Powered Planning',
    description: 'Smart itineraries crafted by AI based on your preferences',
    gradient: 'from-purple-500 to-pink-500'
  },
  {
    icon: Calendar,
    title: 'Manage All Bookings',
    description: 'Track flights, hotels, and activities in one place',
    gradient: 'from-blue-500 to-cyan-500'
  },
  {
    icon: Shield,
    title: 'Secure & Reliable',
    description: 'Your data protected with enterprise-grade security',
    gradient: 'from-green-500 to-emerald-500'
  }
];

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { signInWithGoogle, isAuthenticated, loading } = useAuth();
  const { toast } = useToast();
  const [isSigningIn, setIsSigningIn] = useState(false);
  const [activeSlide, setActiveSlide] = useState(0);
  const isMobile = useMediaQuery('(max-width: 1023px)');

  // Auto-advance carousel on mobile
  useEffect(() => {
    if (!isMobile) return;
    
    const interval = setInterval(() => {
      setActiveSlide((prev) => (prev + 1) % FEATURES.length);
    }, 4000);
    
    return () => clearInterval(interval);
  }, [isMobile]);

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated && !loading) {
      const from = (location.state as any)?.from?.pathname || '/dashboard';
      navigate(from, { replace: true });
    }
  }, [isAuthenticated, loading, navigate, location]);

  const handleGoogleSignIn = async () => {
    try {
      setIsSigningIn(true);
      await signInWithGoogle();
      toast({
        title: 'Welcome back!',
        description: 'You have successfully signed in.',
      });
    } catch (error: any) {
      console.error('Sign in error:', error);
      toast({
        title: 'Sign in failed',
        description: error.message || 'Please try again later.',
        variant: 'destructive',
      });
    } finally {
      setIsSigningIn(false);
    }
  };



  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-indigo-50">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

  // Mobile View with Glassmorphism
  if (isMobile) {
    return (
      <div className="h-screen bg-gradient-to-br from-primary to-blue-600 relative overflow-hidden flex items-center justify-center">
        {/* Animated Background Blobs for contrast */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-400/30 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-blue-800/30 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }} />
        <div className="absolute top-1/3 right-1/4 w-64 h-64 bg-white/10 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }} />

        <div className="relative z-10 w-full max-w-md px-4">
          {/* Sign In Card with Glassmorphism - Primary Focus */}
          <div className="bg-white/15 backdrop-blur-2xl border border-white/30 rounded-3xl p-8 shadow-2xl mb-6">
            {/* Logo */}
          <div className="flex flex-col items-center mb-8">
            <div className="w-20 h-20 rounded-2xl bg-white/20 backdrop-blur-xl border border-white/30 flex items-center justify-center shadow-2xl mb-4">
              <Plane className="w-10 h-10 text-white" />
            </div>
            <h1 className="text-4xl font-bold text-primary mb-2">
              Easy<span className="text-primary">Trip</span>
            </h1>
            <p className="text-white/80 text-sm">Your AI Travel Companion</p>
          </div>

            {/* Google Sign In Button */}
            <Button
              onClick={handleGoogleSignIn}
              disabled={isSigningIn}
              className="w-full h-14 text-base font-semibold bg-white text-primary hover:bg-white/95 shadow-xl hover:shadow-2xl transition-all duration-300 touch-manipulation active:scale-95"
            >
              {isSigningIn ? (
                <>
                  <div className="w-7 h-7 border-2 border-primary border-t-transparent rounded-full animate-spin mr-2" />
                  Signing in...
                </>
              ) : (
                <>
                  <svg className="w-5 h-5 mr-2" viewBox="0 0 24 24">
                    <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                    <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                    <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                    <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                  </svg>
                  Sign in with Google
                </>
              )}
            </Button>
          </div>

          {/* Feature Carousel - Compact */}
          <div className="relative">
            <div className="overflow-hidden rounded-2xl">
              <div 
                className="flex transition-transform duration-500 ease-out"
                style={{ transform: `translateX(-${activeSlide * 100}%)` }}
              >
                {FEATURES.map((feature, index) => (
                  <div key={index} className="w-full flex-shrink-0 px-1">
                    <div className="bg-white/10 backdrop-blur-xl border border-white/20 rounded-2xl p-4 shadow-xl">
                      <div className="flex items-center gap-3">
                        <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center flex-shrink-0">
                          <feature.icon className="w-6 h-6 text-white" />
                        </div>
                        <div className="flex-1 min-w-0">
                          <h3 className="text-sm font-bold text-white mb-0.5">{feature.title}</h3>
                          <p className="text-xs text-white/80 line-clamp-2">{feature.description}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Dots Indicator */}
            <div className="flex justify-center gap-2 mt-4">
              {FEATURES.map((_, index) => (
                <button
                  key={index}
                  onClick={() => setActiveSlide(index)}
                  className={cn(
                    "h-1.5 rounded-full transition-all duration-300 touch-manipulation",
                    activeSlide === index 
                      ? "w-6 bg-white" 
                      : "w-1.5 bg-white/40"
                  )}
                />
              ))}
            </div>
          </div>

          
        </div>
      </div>
    );
  }

  // Desktop View (Original Design)
  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-indigo-50 flex flex-col lg:flex-row relative">
      {/* Centered Logo - Absolute positioned in middle */}
      <div className="hidden lg:flex absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 flex-col items-center gap-6">
        {/* Logo Card with Glass Effect */}
        <div className="p-8 rounded-3xl bg-white/20 backdrop-blur-xl border border-white/30 shadow-2xl">
          <div className="flex flex-col items-center">
            <div className="w-20 h-20 rounded-2xl bg-gradient-to-br from-white to-blue-100 flex items-center justify-center shadow-lg mb-4">
              <Plane className="w-10 h-10 text-primary" />
            </div>
            <h1 className="text-4xl font-bold">
              <span className="text-gray-900">Easy</span>
              <span className="text-primary">Trip</span>
            </h1>
          </div>
        </div>

        {/* Trust Badge */}
        <div className="px-6 py-3 rounded-full bg-white/20 backdrop-blur-xl border border-white/30 shadow-lg">
          <div className="flex items-center gap-2 text-gray-900">
            <CheckCircle2 className="w-4 h-4" />
            <span className="text-sm font-medium">Trusted by 10K+ travelers</span>
          </div>
        </div>
      </div>

      {/* Left Side - Features */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary to-blue-600 p-8 items-center justify-center relative overflow-hidden">
        {/* Decorative elements */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-white/5 rounded-full blur-3xl" />

        <div className="relative z-10 w-full max-w-lg">
          <div className="space-y-6">
            {FEATURES.map((feature, index) => (
              <div
                key={index}
                className="flex gap-4 p-5 rounded-2xl bg-white/10 backdrop-blur-sm border border-white/20 hover:bg-white/15 transition-all duration-300 h-[140px] items-center"
              >
                <div className="flex-shrink-0">
                  <div className="w-12 h-12 rounded-xl bg-white/20 flex items-center justify-center">
                    <feature.icon className="w-6 h-6 text-white" />
                  </div>
                </div>
                <div>
                  <h3 className="font-semibold text-white text-lg mb-1">{feature.title}</h3>
                  <p className="text-sm text-white/80">{feature.description}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right Side - Sign In */}
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-8 min-h-screen">
        <div className="w-full max-w-xl">
          <Card className="p-8 shadow-2xl border-0 bg-white h-[468px] flex flex-col justify-center">
            {/* Header */}
            <div className="text-center mb-8">
              <h2 className="text-3xl font-bold text-gray-900 mb-2">Welcome Back</h2>
              <p className="text-base text-muted-foreground">
                Sign in to start planning your next adventure
              </p>
            </div>

            {/* Google Sign In Button */}
            <Button
              onClick={handleGoogleSignIn}
              disabled={isSigningIn}
              size="lg"
              className="w-full h-16 text-lg font-semibold shadow-lg hover:shadow-2xl transition-all duration-300"
            >
              {isSigningIn ? (
                <>
                  <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin mr-3" />
                  Signing in...
                </>
              ) : (
                <>
                  <svg className="w-8 h-8 mr-3" viewBox="0 0 24 24">
                    <path fill="currentColor" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                    <path fill="currentColor" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                    <path fill="currentColor" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                    <path fill="currentColor" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                  </svg>
                  Continue with Google
                </>
              )}
            </Button>

            {/* Privacy Notice */}
            <p className="mt-6 text-xs text-center text-muted-foreground">
              By continuing, you agree to our{' '}
              <a href="/terms" className="text-primary hover:underline font-medium">
                Terms of Service
              </a>
              {' and '}
              <a href="/privacy" className="text-primary hover:underline font-medium">
                Privacy Policy
              </a>
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
