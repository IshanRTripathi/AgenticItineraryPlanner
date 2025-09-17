import React, { useState } from 'react';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Badge } from './ui/badge';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogTrigger } from './ui/dialog';
import { 
  MapPin, 
  Calendar, 
  Users, 
  ArrowRight, 
  Globe,
  FileText,
  Save,
  Smartphone,
  DollarSign,
  BookOpen,
  Video,
  Instagram,
  Facebook,
  Linkedin,
  Youtube,
  Download,
  X
} from 'lucide-react';
import { TripData } from '../App';
import heroImage1 from 'figma:asset/784212bd1b7f49a82ac20d9447d65decf6e3d3b5.png';
import heroImage2 from 'figma:asset/bb7fcbdd7b165c123b2c735f821bb96218785648.png';
import heroImage3 from 'figma:asset/11971755929e79794c06b2057153b16a0abdbf87.png';

interface LandingPageProps {
  isAuthenticated: boolean;
  onAuthenticate: () => void;
  onStartTrip: () => void;
  onViewTrips: () => void;
  trips: TripData[];
}

export function LandingPage({ 
  isAuthenticated, 
  onAuthenticate, 
  onStartTrip, 
  onViewTrips,
  trips 
}: LandingPageProps) {
  const [showTripModal, setShowTripModal] = useState(false);

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="px-6 lg:px-12 h-16 flex items-center justify-between bg-white border-b border-gray-100">
        <div className="flex items-center space-x-8">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-r from-teal-400 to-teal-600 rounded-lg flex items-center justify-center">
              <span className="text-white font-bold text-sm">S</span>
            </div>
            <span className="text-xl font-semibold text-gray-900">stippl.</span>
          </div>
          
          <nav className="hidden md:flex space-x-8">
            <a href="#" className="text-gray-600 hover:text-gray-900 transition-colors">Product</a>
            <a href="#" className="text-gray-600 hover:text-gray-900 transition-colors">Creators</a>
            <a href="#" className="text-gray-600 hover:text-gray-900 transition-colors">Blog</a>
          </nav>
        </div>
        
        <div className="flex items-center space-x-4">
          {!isAuthenticated ? (
            <>
              <Button variant="ghost" onClick={onAuthenticate} className="text-gray-600">
                Login
              </Button>
              <Button 
                onClick={onAuthenticate}
                className="bg-teal-500 hover:bg-teal-600 text-white px-6 rounded-full"
              >
                Sign up free
              </Button>
            </>
          ) : (
            <Button variant="outline" onClick={onViewTrips} className="rounded-full">
              My Trips
            </Button>
          )}
        </div>
      </header>

      {/* Hero Section 1 - Start your journey */}
      <section className="relative px-6 lg:px-12 py-20 bg-gradient-to-br from-gray-50 to-white overflow-hidden">
        {/* Decorative Elements */}
        <div className="absolute left-0 top-1/2 transform -translate-y-1/2 -translate-x-1/4 opacity-60">
          <div className="w-80 h-80 bg-gradient-to-br from-pink-200 to-orange-200 rounded-full blur-3xl"></div>
          <div className="absolute top-10 left-10 w-20 h-20 bg-orange-300 rounded-lg transform rotate-12"></div>
          <div className="absolute top-20 left-32 w-16 h-16 bg-pink-300 rounded-full"></div>
        </div>
        
        <div className="absolute right-0 top-1/2 transform -translate-y-1/2 translate-x-1/4 opacity-60">
          <div className="w-80 h-80 bg-gradient-to-br from-teal-200 to-green-200 rounded-full blur-3xl"></div>
          <div className="absolute top-8 right-8 w-24 h-32 bg-teal-400 rounded-lg transform -rotate-12"></div>
          <div className="absolute top-32 right-24 w-12 h-12 bg-green-400 rounded-full"></div>
        </div>

        <div className="max-w-4xl mx-auto text-center relative z-10">
          <p className="text-sm uppercase tracking-wider text-gray-500 mb-4">SIGN UP NOW</p>
          <h1 className="text-5xl lg:text-6xl font-bold text-gray-900 mb-8">
            Start your journey
          </h1>
          
          <Button 
            size="lg" 
            onClick={onStartTrip}
            className="bg-teal-500 hover:bg-teal-600 text-white px-8 py-4 text-lg rounded-full mb-8"
          >
            Sign up free
          </Button>
          
          <p className="text-gray-600 mb-6">or download the app</p>
          
          <div className="flex justify-center space-x-4">
            <Button variant="outline" className="flex items-center space-x-2 px-6 py-3 rounded-xl">
              <div className="w-6 h-6 bg-black rounded-sm flex items-center justify-center">
                <span className="text-white text-xs">🍎</span>
              </div>
              <span>App Store</span>
            </Button>
            <Button variant="outline" className="flex items-center space-x-2 px-6 py-3 rounded-xl">
              <div className="w-6 h-6 bg-gradient-to-r from-yellow-400 to-green-400 rounded-sm flex items-center justify-center">
                <span className="text-white text-xs">▷</span>
              </div>
              <span>Google Play</span>
            </Button>
          </div>
        </div>
      </section>

      {/* Hero Section 2 - One travel app */}
      <section className="relative px-6 lg:px-12 py-20 bg-white overflow-hidden">
        {/* Decorative Elements */}
        <div className="absolute left-0 top-1/2 transform -translate-y-1/2 -translate-x-1/4 opacity-40">
          <div className="w-64 h-64 bg-gradient-to-br from-pink-200 to-orange-200 rounded-full blur-3xl"></div>
        </div>
        
        <div className="absolute right-0 top-1/2 transform -translate-y-1/2 translate-x-1/4 opacity-40">
          <div className="w-64 h-64 bg-gradient-to-br from-teal-200 to-green-200 rounded-full blur-3xl"></div>
        </div>

        <div className="max-w-6xl mx-auto relative z-10">
          <div className="text-center mb-16">
            <h2 className="text-4xl lg:text-5xl font-bold mb-6">
              <span className="text-teal-500">One travel app</span><br />
              <span className="text-gray-900">to replace them all</span>
            </h2>
            <p className="text-xl text-gray-600 max-w-2xl mx-auto mb-8">
              Streamline every aspect of your trip - from itinerary planning and 
              travel budgeting to packing and wanderlust sharing.
            </p>
            <Button 
              size="lg" 
              onClick={onStartTrip}
              className="bg-teal-500 hover:bg-teal-600 text-white px-8 py-4 text-lg rounded-full"
            >
              Get started, it's FREE
            </Button>
          </div>

          {/* Feature Icons */}
          <div className="grid grid-cols-3 md:grid-cols-7 gap-8 mb-16">
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-teal-100 rounded-xl flex items-center justify-center mb-2">
                <FileText className="w-6 h-6 text-teal-600" />
              </div>
              <span className="text-sm text-gray-600">Document</span>
              <span className="text-xs text-teal-500">Planner</span>
            </div>
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mb-2">
                <Save className="w-6 h-6 text-gray-600" />
              </div>
              <span className="text-sm text-gray-600">Save</span>
            </div>
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mb-2">
                <Smartphone className="w-6 h-6 text-gray-600" />
              </div>
              <span className="text-sm text-gray-600">Mobile</span>
            </div>
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mb-2">
                <Calendar className="w-6 h-6 text-gray-600" />
              </div>
              <span className="text-sm text-gray-600">Calendar</span>
            </div>
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mb-2">
                <DollarSign className="w-6 h-6 text-gray-600" />
              </div>
              <span className="text-sm text-gray-600">Budget</span>
            </div>
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mb-2">
                <BookOpen className="w-6 h-6 text-gray-600" />
              </div>
              <span className="text-sm text-gray-600">Journal</span>
            </div>
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 bg-gray-100 rounded-xl flex items-center justify-center mb-2">
                <Video className="w-6 h-6 text-gray-600" />
              </div>
              <span className="text-sm text-gray-600">Travel video</span>
            </div>
          </div>

          {/* App Screenshots */}
          <div className="grid md:grid-cols-2 gap-8 items-center">
            <div className="space-y-4">
              <Card className="p-4 bg-gray-50">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center space-x-2">
                    <div className="w-8 h-8 bg-teal-500 rounded-full"></div>
                    <div>
                      <p className="font-medium">An Italian Adventure ✨</p>
                      <p className="text-sm text-gray-500">Rome</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-bold">€ 4,558</p>
                    <p className="text-sm text-gray-500">12/1h nights</p>
                  </div>
                </div>
                <div className="space-y-2">
                  <div className="flex items-center justify-between p-2 bg-white rounded">
                    <span className="text-sm">🚗 DESTINATION</span>
                    <span className="text-sm">💳 BUDGET</span>
                    <span className="text-sm">🏨 SHOPPING</span>
                    <span className="text-sm">👥 TRANSPORT</span>
                  </div>
                </div>
              </Card>
            </div>
            <div className="bg-teal-500 rounded-3xl p-6 text-white relative overflow-hidden">
              <div className="absolute top-4 right-4">
                <Button size="sm" variant="secondary" className="rounded-full">
                  View
                </Button>
              </div>
              <h3 className="text-xl font-bold mb-2">Adventure in Thailand</h3>
              <p className="text-teal-100 mb-4">A long weekend in Krabi</p>
              <div className="space-y-2">
                <div className="flex items-center space-x-2">
                  <MapPin className="w-4 h-4" />
                  <span className="text-sm">13km</span>
                </div>
                <div className="bg-white/20 rounded-lg p-3 backdrop-blur-sm">
                  <p className="text-sm">Thailand</p>
                  <p className="text-xs text-teal-100">2 years</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Hero Section 3 - AI Travel Planner */}
      <section className="relative px-6 lg:px-12 py-20 bg-gradient-to-br from-purple-50 to-teal-50 overflow-hidden">
        {/* Decorative Elements */}
        <div className="absolute left-0 top-0 opacity-30">
          <div className="w-96 h-96 border-4 border-teal-300 rounded-full"></div>
          <div className="absolute top-20 left-20 w-64 h-64 border-4 border-purple-300 rounded-full"></div>
        </div>
        
        <div className="absolute right-0 bottom-0 opacity-30">
          <div className="w-96 h-96 border-4 border-green-300 rounded-full"></div>
        </div>

        <div className="max-w-4xl mx-auto text-center relative z-10">
          <p className="text-sm uppercase tracking-wider text-purple-600 mb-4">YOUR TRIP IN JUST 2 MINUTES</p>
          <h2 className="text-4xl lg:text-5xl font-bold mb-6">
            Your <span className="text-purple-600">AI</span> travel planner
          </h2>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto mb-8">
            Generate tailored itineraries in a matter of minutes. Use and personalize 
            them to your needs. Generate day-by-day notes for each destination.
          </p>
          
          <Dialog open={showTripModal} onOpenChange={setShowTripModal}>
            <DialogTrigger asChild>
              <Button 
                size="lg" 
                className="bg-purple-600 hover:bg-purple-700 text-white px-8 py-4 text-lg rounded-full mb-8"
              >
                Learn more
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>AI Trip Generation</DialogTitle>
                <DialogDescription>
                  See how our AI creates personalized travel itineraries tailored to your preferences and travel style.
                </DialogDescription>
              </DialogHeader>
              <div className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <Button variant="ghost" size="sm">
                    <ArrowRight className="w-4 h-4 rotate-180 mr-2" />
                    Generate Trip
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => setShowTripModal(false)}>
                    <X className="w-4 h-4" />
                  </Button>
                </div>
                
                <div className="grid md:grid-cols-2 gap-8">
                  <div>
                    <div className="bg-purple-100 rounded-2xl p-6 mb-6">
                      <div className="w-16 h-16 bg-purple-600 rounded-full flex items-center justify-center mx-auto mb-4">
                        <div className="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin"></div>
                      </div>
                      <h3 className="text-lg font-semibold text-center text-purple-900">
                        Your trip is being generated
                      </h3>
                    </div>
                    
                    <div className="space-y-4">
                      <div className="flex items-center space-x-3 p-3 bg-white rounded-lg border">
                        <div className="w-8 h-8 bg-orange-100 rounded-full flex items-center justify-center">
                          <span className="text-orange-600">💰</span>
                        </div>
                        <span className="font-medium">Relax</span>
                      </div>
                      <div className="flex items-center space-x-3 p-3 bg-white rounded-lg border">
                        <div className="w-8 h-8 bg-blue-100 rounded-full flex items-center justify-center">
                          <span className="text-blue-600">🎨</span>
                        </div>
                        <span className="font-medium">Culture</span>
                      </div>
                      <div className="flex items-center space-x-3 p-3 bg-white rounded-lg border">
                        <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center">
                          <span className="text-green-600">🏔️</span>
                        </div>
                        <span className="font-medium">Adventure</span>
                      </div>
                      <div className="flex items-center space-x-3 p-3 bg-white rounded-lg border">
                        <div className="w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                          <span className="text-red-600">👥</span>
                        </div>
                        <span className="font-medium">Off the beaten track</span>
                      </div>
                    </div>
                  </div>
                  
                  <div className="bg-white rounded-2xl p-6 border">
                    <h4 className="font-semibold mb-4">What kind of trip are you looking for?</h4>
                    <p className="text-sm text-gray-600 mb-4">
                      Go for total relaxation, culture or beautiful adventures.
                    </p>
                    
                    <div className="space-y-3">
                      <div className="p-4 border rounded-lg">
                        <p className="font-medium mb-2">RELAX</p>
                        <p className="text-sm text-gray-600">
                          Hit the Amalfi Coast if you want to zone out in total bliss from the beauty that surrounds you...
                        </p>
                      </div>
                      
                      <div className="p-4 border rounded-lg bg-blue-50">
                        <p className="font-medium mb-2">VATICAN CITY DISCOVERY</p>
                        <p className="text-sm text-gray-600">
                          Vatican City's Sistine Chapel tour. Each sector due to exploring the Chapel...
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="mt-8 text-center">
                  <Button 
                    onClick={() => {
                      setShowTripModal(false);
                      onStartTrip();
                    }}
                    className="bg-teal-500 hover:bg-teal-600 text-white px-8 py-3 rounded-full"
                  >
                    Start Planning Your Trip
                  </Button>
                </div>
              </div>
            </DialogContent>
          </Dialog>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-50 px-6 lg:px-12 py-16">
        <div className="max-w-6xl mx-auto">
          <div className="grid md:grid-cols-5 gap-8 mb-12">
            <div>
              <div className="flex items-center space-x-2 mb-4">
                <div className="w-8 h-8 bg-gradient-to-r from-teal-400 to-teal-600 rounded-lg flex items-center justify-center">
                  <span className="text-white font-bold text-sm">S</span>
                </div>
                <span className="text-xl font-semibold text-gray-900">stippl.</span>
              </div>
              <div className="flex space-x-3">
                <Button variant="ghost" size="sm" className="p-2">
                  <Instagram className="w-4 h-4" />
                </Button>
                <Button variant="ghost" size="sm" className="p-2">
                  <Facebook className="w-4 h-4" />
                </Button>
                <Button variant="ghost" size="sm" className="p-2">
                  <Linkedin className="w-4 h-4" />
                </Button>
                <Button variant="ghost" size="sm" className="p-2">
                  <Youtube className="w-4 h-4" />
                </Button>
              </div>
            </div>
            
            <div>
              <h4 className="font-semibold text-gray-900 mb-4">Product</h4>
              <div className="space-y-2">
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Itinerary planner</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Budget planner</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Packing list</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Your map</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Travel profile</a>
              </div>
            </div>
            
            <div>
              <h4 className="font-semibold text-gray-900 mb-4">Work with us</h4>
              <div className="space-y-2">
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Travel bloggers</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Influencers</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">DMCs</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Travel agents</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Hotels</a>
              </div>
            </div>
            
            <div>
              <h4 className="font-semibold text-gray-900 mb-4">Resources</h4>
              <div className="space-y-2">
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Blog</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">About us</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Give us feedback</a>
                <a href="#" className="block text-gray-600 hover:text-gray-900 transition-colors">Contact us</a>
              </div>
            </div>
            
            <div>
              <h4 className="font-semibold text-gray-900 mb-4">Download the app</h4>
              <div className="space-y-3">
                <Button variant="outline" className="w-full justify-start">
                  <div className="w-6 h-6 bg-black rounded-sm flex items-center justify-center mr-2">
                    <span className="text-white text-xs">🍎</span>
                  </div>
                  App Store
                </Button>
                <Button variant="outline" className="w-full justify-start">
                  <div className="w-6 h-6 bg-gradient-to-r from-yellow-400 to-green-400 rounded-sm flex items-center justify-center mr-2">
                    <span className="text-white text-xs">▷</span>
                  </div>
                  Google Play
                </Button>
              </div>
            </div>
          </div>
          
          <div className="border-t border-gray-200 pt-8 text-center text-gray-600">
            <p>&copy; 2024 Stippl. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}