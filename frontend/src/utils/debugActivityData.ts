/**
 * Debug utility to check activity card data structure
 * Use this in browser console to diagnose missing data
 */

export const debugActivityData = (itinerary: any) => {
  console.group('🔍 Activity Card Data Debug');
  
  const days = itinerary?.itinerary?.days || [];
  console.log('Total days:', days.length);
  
  if (days.length === 0) {
    console.warn('⚠️ No days found in itinerary');
    console.groupEnd();
    return;
  }
  
  const firstDay = days[0];
  const activities = firstDay.nodes || firstDay.components || [];
  
  console.log('Day 1 activities:', activities.length);
  
  if (activities.length === 0) {
    console.warn('⚠️ No activities found in first day');
    console.groupEnd();
    return;
  }
  
  const firstActivity = activities[0];
  
  console.group('📍 First Activity Analysis');
  console.log('Title:', firstActivity.title || firstActivity.name);
  console.log('Type:', firstActivity.type);
  
  console.group('🗺️ Location Data');
  const loc = firstActivity.location;
  if (!loc) {
    console.error('❌ No location object found!');
  } else {
    console.log('✅ Location exists');
    console.log('  - placeId:', loc.placeId || '❌ Missing');
    console.log('  - rating:', loc.rating || '❌ Missing');
    console.log('  - userRatingsTotal:', loc.userRatingsTotal || '❌ Missing');
    console.log('  - priceLevel:', loc.priceLevel || '❌ Missing');
    console.log('  - photos:', loc.photos?.length || 0, 'photos');
    console.log('  - address:', loc.address || '❌ Missing');
    
    if (loc.photos && loc.photos.length > 0) {
      console.log('  - First photo ref:', loc.photos[0].substring(0, 20) + '...');
    }
  }
  console.groupEnd();
  
  console.group('🔑 Environment Check');
  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY;
  if (apiKey) {
    console.log('✅ API Key is set:', apiKey.substring(0, 10) + '...');
  } else {
    console.error('❌ API Key is MISSING!');
    console.log('Expected env var: VITE_GOOGLE_MAPS_BROWSER_KEY');
  }
  console.groupEnd();
  
  console.group('🖼️ Photo URL Test');
  if (loc?.photos?.[0] && apiKey) {
    const photoUrl = `https://maps.googleapis.com/maps/api/place/photo?maxwidth=200&photo_reference=${loc.photos[0]}&key=${apiKey}`;
    console.log('Generated URL:', photoUrl.substring(0, 100) + '...');
    console.log('Test this URL in a new tab to verify it works');
  } else {
    console.warn('⚠️ Cannot generate photo URL - missing data or API key');
  }
  console.groupEnd();
  
  console.groupEnd();
  console.groupEnd();
  
  return {
    hasLocation: !!loc,
    hasPlaceId: !!loc?.placeId,
    hasRating: !!loc?.rating,
    hasReviews: !!loc?.userRatingsTotal,
    hasPhotos: !!(loc?.photos && loc.photos.length > 0),
    hasPriceLevel: !!loc?.priceLevel,
    hasApiKey: !!apiKey,
    photoCount: loc?.photos?.length || 0,
    rating: loc?.rating,
    reviews: loc?.userRatingsTotal
  };
};

// Make it available globally for easy console access
if (typeof window !== 'undefined') {
  (window as any).debugActivityData = debugActivityData;
}
