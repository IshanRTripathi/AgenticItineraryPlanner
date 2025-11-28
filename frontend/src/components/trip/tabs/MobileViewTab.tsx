import { useState, useEffect, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { fetchWeatherForecast } from '@/services/weatherService';
import { exportService } from '@/services/exportService';
import { useToast } from '@/components/ui/use-toast';
import { useTranslation } from '@/i18n';
import { useCurrency } from '@/hooks/useCurrency';
import { useAuth } from '@/contexts/AuthContext';
import { ExportOptionsModal, ExportOptions } from '@/components/export/ExportOptionsModal';
import { ShareModal } from '@/components/share/ShareModal';
import {
    Calendar,
    MapPin,
    Coins,
    Cloud,
    Share2,
    Download,
    ArrowRight
} from 'lucide-react';

interface PhotoWithPlace {
    photoRef: string;
    placeName: string;
    placeType?: string;
}

interface MobileViewTabProps {
    itinerary: any;
}

export function MobileViewTab({ itinerary }: MobileViewTabProps) {
    const { t } = useTranslation();
    const { preferredCurrency, convert, getCurrencySymbol } = useCurrency();
    const { user, isAuthenticated } = useAuth();
    const { toast } = useToast();

    const [currentWeather, setCurrentWeather] = useState<{
        high: number;
        low: number;
        condition: string;
        icon: string;
    } | null>(null);

    // Slideshow state
    const [photos, setPhotos] = useState<PhotoWithPlace[]>([]);
    const [currentPhotoIndex, setCurrentPhotoIndex] = useState(0);

    const [isExportModalOpen, setIsExportModalOpen] = useState(false);
    const [isShareModalOpen, setIsShareModalOpen] = useState(false);
    const [isExporting, setIsExporting] = useState(false);

    // --- Data Extraction & Calculation ---

    const getDestinationCity = () => {
        if (itinerary.destination) return itinerary.destination.split(',')[0].trim();
        if (itinerary.days?.[0]?.location) return itinerary.days[0].location.split(',')[0].trim();
        if (itinerary.summary) {
            const match = itinerary.summary.match(/for\s+([^,]+)/);
            if (match) return match[1].trim();
        }
        return 'Unknown';
    };

    const destination = getDestinationCity();
    const days = itinerary?.itinerary?.days || itinerary?.days || [];
    const startDate = days[0]?.date || '';
    const endDate = days[days.length - 1]?.date || '';
    const dayCount = days.length;

    const activityCount = days.reduce((total: number, day: any) => {
        return total + (day.nodes || []).length;
    }, 0);

    const totalBudget = days.reduce((total: number, day: any) => {
        const nodes = day.nodes || [];
        return total + nodes.reduce((daySum: number, node: any) => {
            const cost = node.cost?.amountPerPerson || node.cost?.pricePerPerson || node.cost?.amount || 0;
            return daySum + (typeof cost === 'number' ? cost : 0);
        }, 0);
    }, 0);

    const itineraryCurrency = itinerary?.currency ||
        days.flatMap((day: any) => day.nodes || [])
            .find((node: any) => node.cost?.currency)?.cost?.currency ||
        'USD';

    const displayCurrency = useMemo(() => preferredCurrency || itineraryCurrency, [preferredCurrency, itineraryCurrency]);

    const convertedTotalBudget = useMemo(() => {
        return convert(totalBudget, itineraryCurrency, displayCurrency);
    }, [totalBudget, itineraryCurrency, displayCurrency, convert]);

    const formatCost = (amount: number) => {
        if (amount >= 1000) {
            return (amount / 1000).toFixed(1).replace(/\.0$/, '') + 'k';
        }
        return amount.toString();
    };

    // --- Effects ---

    // Fetch Weather
    useEffect(() => {
        async function loadWeather() {
            if (!destination || destination === 'Unknown') return;
            try {
                const forecast = await fetchWeatherForecast(destination, 1);
                if (forecast.length > 0) {
                    setCurrentWeather({
                        high: forecast[0].high,
                        low: forecast[0].low,
                        condition: forecast[0].description,
                        icon: forecast[0].icon,
                    });
                }
            } catch (error) {
                console.error('[MobileViewTab] Failed to load weather:', error);
            }
        }
        loadWeather();
    }, [destination]);

    // Fetch Photos & Slideshow
    useEffect(() => {
        if (photos.length > 0) return;

        const allPhotos: PhotoWithPlace[] = [];
        for (const day of days) {
            const nodes = day.nodes || [];
            for (const node of nodes) {
                const isAttraction = node.type === 'attraction' ||
                    node.type === 'activity' ||
                    node.type === 'sightseeing' ||
                    (!node.type?.includes('hotel') && !node.type?.includes('accommodation'));

                if (isAttraction && node.location?.photos && node.location.photos.length > 0) {
                    allPhotos.push({
                        photoRef: node.location.photos[0],
                        placeName: node.title || node.location?.name || 'Unknown Place',
                        placeType: node.type
                    });
                }
            }
        }

        if (allPhotos.length > 0) {
            // Shuffle array for variety
            const shuffled = allPhotos.sort(() => 0.5 - Math.random());
            setPhotos(shuffled.slice(0, 10)); // Keep top 10
        } else {
            const searchQuery = encodeURIComponent(destination);
            setPhotos([{
                photoRef: `https://source.unsplash.com/1600x900/?${searchQuery},travel,landmark`,
                placeName: destination,
                placeType: 'destination'
            }]);
        }
    }, [days, destination]);

    // Auto-advance slideshow
    useEffect(() => {
        if (photos.length <= 1) return;

        const interval = setInterval(() => {
            setCurrentPhotoIndex((prev) => (prev + 1) % photos.length);
        }, 5000); // Change every 5 seconds

        return () => clearInterval(interval);
    }, [photos]);

    const handleExport = async (options: ExportOptions) => {
        setIsExporting(true);
        try {
            await exportService.exportToPDF(itinerary as any);
            toast({ title: 'Export successful', description: 'Your itinerary is ready.' });
            setIsExportModalOpen(false);
        } catch (error) {
            toast({ title: 'Export failed', description: 'Could not export PDF.', variant: 'destructive' });
        } finally {
            setIsExporting(false);
        }
    };

    const getPhotoUrl = (photoRef?: string) => {
        if (!photoRef) return '';
        if (photoRef.startsWith('http')) return photoRef;
        return `https://maps.googleapis.com/maps/api/place/photo?maxwidth=1200&photo_reference=${photoRef}&key=${import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY}`;
    };

    const currentPhoto = photos[currentPhotoIndex];

    return (
        <div className="fixed inset-0 z-10 bg-slate-900 text-white overflow-y-auto font-sans no-scrollbar">

            {/* 1. Immersive Background Slideshow */}
            <div className="fixed inset-0 z-0">
                <AnimatePresence mode="wait">
                    {currentPhoto ? (
                        <motion.img
                            key={currentPhoto.photoRef}
                            src={getPhotoUrl(currentPhoto.photoRef)}
                            alt={destination}
                            className="w-full h-full object-cover"
                            initial={{ scale: 1.1, opacity: 0 }}
                            animate={{ scale: 1, opacity: 1 }}
                            exit={{ opacity: 0 }}
                            transition={{ duration: 1.5, ease: "easeOut" }}
                        />
                    ) : (
                        <div className="w-full h-full bg-slate-900" />
                    )}
                </AnimatePresence>

                {/* Stronger Gradient Overlays for Readability */}
                <div className="absolute inset-0 bg-gradient-to-b from-black/70 via-transparent to-black/90" />
                <div className="absolute inset-0 bg-gradient-to-t from-slate-950 via-slate-950/50 to-transparent opacity-80" />
            </div>

            {/* 2. Content Container */}
            <div className="relative z-10 flex flex-col min-h-screen px-6 pt-10 pb-8">

                {/* Header - Clean, no boxes */}
                <motion.div
                    className="flex justify-between items-start mb-8"
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.2 }}
                >
                    <div>
                        <p className="text-slate-300 font-medium text-[10px] uppercase tracking-widest mb-0.5 drop-shadow-md">Welcome back</p>
                        <h2 className="text-xl font-bold text-white tracking-tight drop-shadow-md">
                            {isAuthenticated && user?.displayName ? user.displayName.split(' ')[0] : 'Traveler'}
                        </h2>
                    </div>

                    <div className="flex flex-col items-end gap-3">
                        {currentWeather && (
                            <div className="flex items-center gap-2 drop-shadow-md">
                                <Cloud className="w-6 h-6 text-blue-400" />
                                <span className="text-lg font-bold">{Math.round(currentWeather.high)}°</span>
                            </div>
                        )}

                        {/* Quick Actions (Share/Export) - Vertical Glass Stack */}
                        <div className="flex flex-col gap-1 bg-black/20 backdrop-blur-md rounded-2xl p-1 border border-white/10">
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setIsShareModalOpen(true)}
                                className="w-7 h-7 rounded-xl text-white hover:bg-white/10 transition-colors"
                            >
                                <Share2 className="w-4 h-4" />
                            </Button>
                            <div className="h-px bg-white/10 mx-1" />
                            <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setIsExportModalOpen(true)}
                                className="w-7 h-7 rounded-xl text-white hover:bg-white/10 transition-colors"
                            >
                                <Download className="w-4 h-4" />
                            </Button>
                        </div>
                    </div>
                </motion.div>

                {/* Spacer to push content down */}
                <div className="flex-grow" />

                {/* Hero Section - Clean, left-aligned */}
                <motion.div
                    className="mb-8 flex flex-col items-start"
                    initial={{ opacity: 0, y: 30 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.4 }}
                >
                    <motion.div
                        initial={{ width: 0 }}
                        animate={{ width: "40px" }}
                        transition={{ delay: 0.6, duration: 0.8 }}
                        className="h-1 bg-emerald-500 mb-4 rounded-full shadow-[0_0_10px_rgba(16,185,129,0.5)]"
                    />
                    <h1
                        className="font-bold text-white leading-[0.9] mb-3 tracking-tight drop-shadow-lg"
                        style={{ fontFamily: "'Roothinkyu', 'Playfair Display', serif", fontSize: '3.9rem' }}
                    >
                        {destination}
                    </h1>

                    <div className="flex items-center gap-4 text-slate-200 text-sm font-medium drop-shadow-md">
                        <div className="flex items-center gap-1.5">
                            <Calendar className="w-3.5 h-3.5 text-emerald-400" />
                            <span>{new Date(startDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })} - {new Date(endDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })}</span>
                        </div>
                        <span className="w-1 h-1 bg-slate-400 rounded-full" />
                        <span>{dayCount} Days</span>
                    </div>
                </motion.div>

                {/* Unified Glass Stats Bar - Left-aligned */}
                <motion.div
                    className="mb-24 bg-white/5 backdrop-blur-md border border-white/10 rounded-3xl overflow-hidden shadow-lg"
                    initial={{ opacity: 0, y: 40 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.6 }}
                >
                    <div className="grid grid-cols-3 divide-x divide-white/10">
                        <div className="p-4 flex flex-col items-start gap-1">
                            <MapPin className="w-4 h-4 text-emerald-400 mb-0.5" />
                            <span className="text-base font-bold text-white">{activityCount}</span>
                            <span className="text-[9px] text-slate-400 uppercase tracking-wider font-medium">Places</span>
                        </div>

                        <div className="p-4 flex flex-col items-start gap-1">
                            <Coins className="w-4 h-4 text-amber-400 mb-0.5" />
                            <span className="text-base font-bold text-white">{getCurrencySymbol(displayCurrency)}{formatCost(Math.round(convertedTotalBudget))}</span>
                            <span className="text-[9px] text-slate-400 uppercase tracking-wider font-medium">Budget</span>
                        </div>

                        <div className="p-4 flex flex-col items-start gap-1">
                            <Cloud className="w-4 h-4 text-blue-400 mb-0.5" />
                            <span className="text-base font-bold text-white">{currentWeather ? `${Math.round(currentWeather.high)}°` : '--'}</span>
                            <span className="text-[9px] text-slate-400 uppercase tracking-wider font-medium">Weather</span>
                        </div>
                    </div>
                </motion.div>

            </div>

            {/* Modals */}
            <ExportOptionsModal
                isOpen={isExportModalOpen}
                onClose={() => setIsExportModalOpen(false)}
                onExport={handleExport}
                isExporting={isExporting}
            />

            <ShareModal
                isOpen={isShareModalOpen}
                onClose={() => setIsShareModalOpen(false)}
                itineraryId={itinerary?.id || itinerary?.itineraryId || ''}
                itinerary={itinerary}
            />
        </div>
    );
}
