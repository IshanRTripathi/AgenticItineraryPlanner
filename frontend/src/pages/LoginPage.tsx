/**
 * Login Page
 * Google Sign-In with feature showcase
 */

import { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { Plane, Sparkles, Shield, Calendar, CheckCircle2 } from 'lucide-react';
import { useToast } from '@/components/ui/use-toast';

const FEATURES = [
  {
    icon: Sparkles,
    title: 'AI-Powered Planning',
    description: 'Smart itineraries crafted by AI based on your preferences'
  },
  {
    icon: Calendar,
    title: 'Manage All Bookings',
    description: 'Track flights, hotels, and activities in one place'
  },
  {
    icon: Shield,
    title: 'Secure & Reliable',
    description: 'Your data protected with enterprise-grade security'
  }
];

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { signInWithGoogle, isAuthenticated, loading } = useAuth();
  const { toast } = useToast();
  const [isSigningIn, setIsSigningIn] = useState(false);

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
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="w-16 h-16 border-4 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4" />
          <p className="text-muted-foreground">Loading...</p>
        </div>
      </div>
    );
  }

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
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-primary to-blue-600 p-12 items-center justify-center relative overflow-hidden">
        {/* Decorative elements */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full blur-3xl" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-white/5 rounded-full blur-3xl" />

        <div className="relative z-10 w-full max-w-lg">
          {/* Features - Each card is 140px (p-5 = 20px top/bottom + content ~100px) */}
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
      <div className="w-full lg:w-1/2 flex items-center justify-center p-6 sm:p-12 min-h-screen">
        <div className="w-full max-w-xl">
          {/* Mobile Logo */}
          <div className="lg:hidden flex flex-col items-center mb-8">
            <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-white to-blue-100 border-2 border-primary/20 flex items-center justify-center shadow-lg mb-3">
              <Plane className="w-8 h-8 text-primary" />
            </div>
            <h1 className="text-3xl font-bold">
              <span className="text-gray-900">Easy</span>
              <span className="text-primary">Trip</span>
            </h1>
          </div>

          {/* Modal height: 3 cards (140px each) + 2 gaps (24px each) = 468px */}
          <Card className="p-12 shadow-2xl border-0 bg-white h-[468px] flex flex-col justify-center">
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
                  <svg className="w-6 h-6 mr-3" viewBox="0 0 24 24">
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

            {/* Mobile Features */}
            <div className="lg:hidden mt-8 pt-8 border-t">
              <div className="space-y-4">
                {FEATURES.map((feature, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg bg-primary/10 flex items-center justify-center flex-shrink-0">
                      <feature.icon className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-gray-900">{feature.title}</p>
                      <p className="text-xs text-muted-foreground">{feature.description}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}

export default LoginPage;
