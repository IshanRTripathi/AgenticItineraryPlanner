/**
 * Export Service
 * Handles PDF export and share functionality for itineraries
 * Enhanced with premium cover page and comprehensive trip information
 */

import { NormalizedItinerary } from '@/types/dto';

class ExportService {
  /**
   * Generate PDF from itinerary
   * Uses browser print API with enhanced layout and comprehensive information
   */
  async exportToPDF(itinerary: NormalizedItinerary): Promise<void> {
    try {
      // Create a printable version of the itinerary
      const printWindow = window.open('', '_blank');
      if (!printWindow) {
        throw new Error('Popup blocked. Please allow popups for this site.');
      }

      const html = this.generatePrintableHTML(itinerary);
      printWindow.document.write(html);
      printWindow.document.close();

      // Wait for content to load (including images)
      printWindow.onload = () => {
        // Small delay to ensure images are loaded
        setTimeout(() => {
          printWindow.print();
        }, 500);
      };
    } catch (error) {
      console.error('Failed to export PDF:', error);
      throw error;
    }
  }

  /**
   * Generate share link for itinerary
   */
  async generateShareLink(itineraryId: string): Promise<string> {
    try {
      // In production, this would call backend API to generate a shareable link
      const baseUrl = window.location.origin;
      const shareUrl = `${baseUrl}/trip/${itineraryId}`;

      // Copy to clipboard
      await navigator.clipboard.writeText(shareUrl);

      return shareUrl;
    } catch (error) {
      console.error('Failed to generate share link:', error);
      throw error;
    }
  }

  /**
   * Share via Web Share API (mobile-friendly)
   */
  async shareViaWebAPI(itinerary: NormalizedItinerary): Promise<void> {
    if (!navigator.share) {
      throw new Error('Web Share API not supported');
    }

    try {
      const days = itinerary.days || [];
      const destination = days[0]?.location || 'Trip';
      const shareData = {
        title: `${destination} Itinerary`,
        text: `Check out my ${destination} travel itinerary!`,
        url: `${window.location.origin}/trip/${itinerary.itineraryId}`,
      };

      await navigator.share(shareData);
    } catch (error) {
      // User cancelled or error occurred
      if ((error as Error).name !== 'AbortError') {
        console.error('Failed to share:', error);
        throw error;
      }
    }
  }

  /**
   * Generate printable HTML for itinerary with premium cover page
   */
  private generatePrintableHTML(itinerary: NormalizedItinerary): string {
    const days = itinerary.days || [];
    const destination = days[0]?.location || itinerary.destination || 'Trip';
    const startDate = days[0]?.date || '';
    const endDate = days[days.length - 1]?.date || '';

    const formatDate = (date: string) => {
      if (!date) return '';
      return new Date(date).toLocaleDateString('en-US', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    };

    const formatDateShort = (date: string) => {
      if (!date) return '';
      return new Date(date).toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      });
    };

    const formatTime = (timestamp: number | string | undefined) => {
      if (!timestamp) return '';
      const date = typeof timestamp === 'number' ? new Date(timestamp) : new Date(timestamp);
      return date.toLocaleTimeString('en-US', {
        hour: 'numeric',
        minute: '2-digit',
        hour12: true,
      });
    };

    const getNodeIcon = (type: string) => {
      const icons: Record<string, string> = {
        attraction: '🏛️',
        place: '🏛️',
        activity: '🎯',
        meal: '🍽️',
        hotel: '🏨',
        accommodation: '🏨',
        transit: '🚗',
        transport: '🚗',
      };
      return icons[type] || '📍';
    };

    // Calculate trip statistics
    const totalActivities = days.reduce((sum, day) => sum + (day.nodes?.length || 0), 0);
    const totalCost = days.reduce((sum, day) => sum + (day.totals?.cost || 0), 0);
    const totalDistance = days.reduce((sum, day) => sum + (day.totals?.distanceKm || 0), 0);
    const bookedCount = days.reduce((sum, day) => 
      sum + (day.nodes?.filter(n => n.bookingRef).length || 0), 0
    );

    // Collect photos from nodes (up to 4 for cover page)
    const coverPhotos: string[] = [];
    for (const day of days) {
      for (const node of day.nodes || []) {
        if (node.location?.photos && node.location.photos.length > 0) {
          coverPhotos.push(...node.location.photos.slice(0, 1));
          if (coverPhotos.length >= 4) break;
        }
      }
      if (coverPhotos.length >= 4) break;
    }

    // Fallback images if no photos available
    while (coverPhotos.length < 4) {
      const searchQuery = encodeURIComponent(destination);
      coverPhotos.push(`https://source.unsplash.com/800x600/?${searchQuery},travel,landmark,${coverPhotos.length}`);
    }

    // Get Google Maps photo URLs
    const getPhotoUrl = (photoRef: string) => {
      if (photoRef.startsWith('http')) return photoRef;
      const apiKey = import.meta.env.VITE_GOOGLE_MAPS_BROWSER_KEY || '';
      return `https://maps.googleapis.com/maps/api/place/photo?maxwidth=800&photo_reference=${photoRef}&key=${apiKey}`;
    };

    // Generate cover page HTML
    const coverPageHTML = `
      <div class="cover-page" style="page-break-after: always; height: 100vh; display: flex; flex-direction: column; position: relative; overflow: hidden;">
        <!-- Photo Grid Background -->
        <div style="position: absolute; top: 0; left: 0; right: 0; height: 60%; display: grid; grid-template-columns: 1fr 1fr; gap: 2px; overflow: hidden;">
          ${coverPhotos.map(photo => `
            <div style="position: relative; overflow: hidden; background: linear-gradient(135deg, #002B5B 0%, #004080 100%);">
              <img src="${getPhotoUrl(photo)}" 
                   style="width: 100%; height: 100%; object-fit: cover; opacity: 0.9;"
                   onerror="this.style.display='none';" />
            </div>
          `).join('')}
        </div>

        <!-- Gradient Overlay -->
        <div style="position: absolute; top: 0; left: 0; right: 0; height: 60%; background: linear-gradient(to bottom, rgba(0,43,91,0.3) 0%, rgba(0,43,91,0.8) 100%);"></div>

        <!-- Content -->
        <div style="position: relative; z-index: 10; flex: 1; display: flex; flex-direction: column; justify-content: space-between; padding: 60px 80px;">
          <!-- Header -->
          <div>
            <div style="display: inline-block; padding: 8px 20px; background: rgba(245,197,66,0.95); border-radius: 20px; margin-bottom: 30px;">
              <span style="color: #002B5B; font-weight: 700; font-size: 14px; letter-spacing: 1px;">YOUR TRAVEL ITINERARY</span>
            </div>
          </div>

          <!-- Main Title -->
          <div style="margin-top: auto; margin-bottom: 40px;">
            <h1 style="color: white; font-size: 72px; font-weight: 800; margin: 0 0 20px 0; line-height: 1.1; text-shadow: 0 4px 20px rgba(0,0,0,0.3);">
              ${destination}
            </h1>
            <div style="display: flex; align-items: center; gap: 30px; flex-wrap: wrap;">
              <div style="display: flex; align-items: center; gap: 10px;">
                <span style="font-size: 24px;">📅</span>
                <span style="color: white; font-size: 20px; font-weight: 500;">${formatDateShort(startDate)} - ${formatDateShort(endDate)}</span>
              </div>
              <div style="display: flex; align-items: center; gap: 10px;">
                <span style="font-size: 24px;">🗓️</span>
                <span style="color: white; font-size: 20px; font-weight: 500;">${days.length} Days</span>
              </div>
              <div style="display: flex; align-items: center; gap: 10px;">
                <span style="font-size: 24px;">🎯</span>
                <span style="color: white; font-size: 20px; font-weight: 500;">${totalActivities} Activities</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Bottom Info Cards -->
        <div style="position: relative; z-index: 10; background: white; padding: 40px 80px;">
          <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 30px;">
            <!-- Total Budget -->
            <div style="text-align: center; padding: 20px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px;">
              <div style="font-size: 32px; margin-bottom: 8px;">💰</div>
              <div style="font-size: 28px; font-weight: 700; color: #002B5B; margin-bottom: 4px;">${itinerary.currency} ${totalCost.toLocaleString()}</div>
              <div style="font-size: 13px; color: #666; font-weight: 500;">Total Budget</div>
            </div>

            <!-- Booked -->
            <div style="text-align: center; padding: 20px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px;">
              <div style="font-size: 32px; margin-bottom: 8px;">✅</div>
              <div style="font-size: 28px; font-weight: 700; color: #10B981; margin-bottom: 4px;">${bookedCount}</div>
              <div style="font-size: 13px; color: #666; font-weight: 500;">Booked</div>
            </div>

            <!-- Distance -->
            <div style="text-align: center; padding: 20px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px;">
              <div style="font-size: 32px; margin-bottom: 8px;">🗺️</div>
              <div style="font-size: 28px; font-weight: 700; color: #002B5B; margin-bottom: 4px;">${totalDistance.toFixed(0)} km</div>
              <div style="font-size: 13px; color: #666; font-weight: 500;">Total Distance</div>
            </div>

            <!-- Days -->
            <div style="text-align: center; padding: 20px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px;">
              <div style="font-size: 32px; margin-bottom: 8px;">⏱️</div>
              <div style="font-size: 28px; font-weight: 700; color: #002B5B; margin-bottom: 4px;">${days.length}</div>
              <div style="font-size: 13px; color: #666; font-weight: 500;">Days</div>
            </div>
          </div>

          <!-- Trip Summary -->
          ${itinerary.summary ? `
            <div style="margin-top: 30px; padding: 25px; background: linear-gradient(135deg, #FFF9E6 0%, #FFF4CC 100%); border-radius: 12px; border-left: 4px solid #F5C542;">
              <div style="font-size: 14px; font-weight: 700; color: #002B5B; margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.5px;">Trip Overview</div>
              <p style="margin: 0; color: #333; font-size: 15px; line-height: 1.6;">${itinerary.summary}</p>
            </div>
          ` : ''}

          <!-- Branding -->
          <div style="margin-top: 30px; text-align: center; padding-top: 20px; border-top: 2px solid #e9ecef;">
            <div style="font-size: 24px; font-weight: 800; color: #002B5B; margin-bottom: 5px;">EaseMyTrip Planner</div>
            <div style="font-size: 13px; color: #666;">Your AI-Powered Travel Companion</div>
          </div>
        </div>
      </div>
    `;

    // Generate day-by-day itinerary HTML
    const daysHTML = days
      .map((day, dayIndex) => `
      <div class="day-page" style="page-break-before: always; padding: 60px 80px;">
        <!-- Day Header -->
        <div style="margin-bottom: 40px; padding-bottom: 20px; border-bottom: 3px solid #F5C542;">
          <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 15px;">
            <div>
              <h2 style="color: #002B5B; font-size: 36px; font-weight: 800; margin: 0 0 10px 0;">
                Day ${day.dayNumber}: ${day.location}
              </h2>
              <p style="color: #666; font-size: 16px; margin: 0;">${formatDate(day.date)}</p>
            </div>
            <div style="text-align: right;">
              ${day.totals?.cost ? `
                <div style="font-size: 24px; font-weight: 700; color: #002B5B;">${itinerary.currency} ${day.totals.cost.toLocaleString()}</div>
                <div style="font-size: 13px; color: #666;">Daily Budget</div>
              ` : ''}
            </div>
          </div>

          <!-- Day Stats -->
          <div style="display: flex; gap: 20px; flex-wrap: wrap;">
            ${day.totals?.distanceKm ? `
              <div style="display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: #f8f9fa; border-radius: 20px;">
                <span style="font-size: 16px;">🗺️</span>
                <span style="font-size: 14px; color: #666; font-weight: 500;">${day.totals.distanceKm.toFixed(1)} km</span>
              </div>
            ` : ''}
            ${day.totals?.durationHr ? `
              <div style="display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: #f8f9fa; border-radius: 20px;">
                <span style="font-size: 16px;">⏱️</span>
                <span style="font-size: 14px; color: #666; font-weight: 500;">${day.totals.durationHr.toFixed(1)} hours</span>
              </div>
            ` : ''}
            <div style="display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: #f8f9fa; border-radius: 20px;">
              <span style="font-size: 16px;">📍</span>
              <span style="font-size: 14px; color: #666; font-weight: 500;">${day.nodes?.length || 0} stops</span>
            </div>
          </div>

          ${(day as any).notes ? `
            <div style="margin-top: 15px; padding: 15px; background: #FFF9E6; border-radius: 8px; border-left: 3px solid #F5C542;">
              <p style="margin: 0; color: #666; font-size: 14px; line-height: 1.5;">📝 ${(day as any).notes}</p>
            </div>
          ` : ''}
        </div>

        <!-- Activities -->
        <div class="activities" style="display: flex; flex-direction: column; gap: 25px;">
          ${(day.nodes || [])
            .map((node, nodeIndex) => {
              const hasPhoto = node.location?.photos && node.location.photos.length > 0;
              const photoUrl = hasPhoto ? getPhotoUrl(node.location!.photos![0]) : '';
              
              return `
            <div class="activity-card" style="display: flex; gap: 20px; padding: 25px; background: white; border: 2px solid #e9ecef; border-radius: 12px; page-break-inside: avoid;">
              <!-- Activity Number Badge -->
              <div style="flex-shrink: 0;">
                <div style="width: 50px; height: 50px; background: linear-gradient(135deg, #002B5B 0%, #004080 100%); border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-weight: 700; font-size: 20px; box-shadow: 0 4px 12px rgba(0,43,91,0.2);">
                  ${nodeIndex + 1}
                </div>
              </div>

              <!-- Activity Content -->
              <div style="flex: 1; min-width: 0;">
                <!-- Header -->
                <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 12px; gap: 15px;">
                  <div style="flex: 1; min-width: 0;">
                    <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
                      <span style="font-size: 28px;">${getNodeIcon(node.type)}</span>
                      <h3 style="margin: 0; color: #002B5B; font-size: 20px; font-weight: 700;">${node.title}</h3>
                      ${node.bookingRef ? '<span style="display: inline-block; padding: 4px 10px; background: #10B981; color: white; border-radius: 12px; font-size: 11px; font-weight: 600; margin-left: 8px;">BOOKED</span>' : ''}
                    </div>
                    
                    ${node.timing?.startTime ? `
                      <div style="display: flex; align-items: center; gap: 6px; color: #666; font-size: 14px; margin-bottom: 4px;">
                        <span>🕐</span>
                        <span style="font-weight: 600;">${formatTime(node.timing.startTime)}</span>
                        ${node.timing.durationMin ? `<span style="color: #999;">• ${node.timing.durationMin} min</span>` : ''}
                      </div>
                    ` : ''}
                  </div>

                  ${node.cost?.amount ? `
                    <div style="text-align: right; flex-shrink: 0;">
                      <div style="font-size: 20px; font-weight: 700; color: #F5C542;">${itinerary.currency} ${node.cost.amount.toLocaleString()}</div>
                      <div style="font-size: 12px; color: #999;">${node.cost.per || 'per person'}</div>
                    </div>
                  ` : ''}
                </div>

                <!-- Location Info -->
                ${node.location?.address ? `
                  <div style="display: flex; align-items: start; gap: 8px; margin-bottom: 10px; padding: 12px; background: #f8f9fa; border-radius: 8px;">
                    <span style="font-size: 16px; flex-shrink: 0;">📍</span>
                    <div style="flex: 1; min-width: 0;">
                      <div style="font-size: 14px; color: #333; font-weight: 500; margin-bottom: 4px;">${node.location.name || node.title}</div>
                      <div style="font-size: 13px; color: #666;">${node.location.address}</div>
                      ${node.location.rating ? `
                        <div style="margin-top: 6px; display: flex; align-items: center; gap: 8px;">
                          <span style="color: #F5C542; font-size: 14px;">⭐ ${node.location.rating.toFixed(1)}</span>
                          ${node.location.userRatingsTotal ? `<span style="color: #999; font-size: 12px;">(${node.location.userRatingsTotal} reviews)</span>` : ''}
                          ${node.location.priceLevel ? `<span style="color: #666; font-size: 14px; margin-left: 8px;">${'$'.repeat(node.location.priceLevel)}</span>` : ''}
                        </div>
                      ` : ''}
                    </div>
                  </div>
                ` : ''}

                <!-- Tips & Recommendations -->
                ${node.tips?.bestTime && node.tips.bestTime.length > 0 ? `
                  <div style="margin-top: 10px; padding: 10px 12px; background: #E6F7FF; border-radius: 8px; border-left: 3px solid #3B82F6;">
                    <div style="font-size: 12px; font-weight: 600; color: #002B5B; margin-bottom: 4px;">💡 BEST TIME TO VISIT</div>
                    <div style="font-size: 13px; color: #333;">${node.tips.bestTime.join(' • ')}</div>
                  </div>
                ` : ''}

                ${node.tips?.warnings && node.tips.warnings.length > 0 ? `
                  <div style="margin-top: 10px; padding: 10px 12px; background: #FFF3E0; border-radius: 8px; border-left: 3px solid #F59E0B;">
                    <div style="font-size: 12px; font-weight: 600; color: #002B5B; margin-bottom: 4px;">⚠️ IMPORTANT</div>
                    <div style="font-size: 13px; color: #333;">${node.tips.warnings.join(' • ')}</div>
                  </div>
                ` : ''}

                <!-- Booking Reference -->
                ${node.bookingRef ? `
                  <div style="margin-top: 10px; padding: 10px 12px; background: #F0FDF4; border-radius: 8px; border-left: 3px solid #10B981;">
                    <div style="font-size: 12px; font-weight: 600; color: #002B5B; margin-bottom: 4px;">🎫 BOOKING CONFIRMATION</div>
                    <div style="font-size: 13px; color: #333; font-family: monospace;">${node.bookingRef}</div>
                  </div>
                ` : ''}

                <!-- Google Maps Link -->
                ${node.location?.placeId ? `
                  <div style="margin-top: 12px;">
                    <a href="https://www.google.com/maps/place/?q=place_id:${node.location.placeId}" 
                       style="display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; background: #002B5B; color: white; text-decoration: none; border-radius: 6px; font-size: 13px; font-weight: 600;">
                      <span>🗺️</span>
                      <span>View on Google Maps</span>
                    </a>
                  </div>
                ` : ''}
              </div>

              <!-- Photo (if available) -->
              ${hasPhoto ? `
                <div style="flex-shrink: 0; width: 180px; height: 180px; border-radius: 8px; overflow: hidden; background: #f0f0f0;">
                  <img src="${photoUrl}" 
                       style="width: 100%; height: 100%; object-fit: cover;"
                       onerror="this.parentElement.style.display='none';" />
                </div>
              ` : ''}
            </div>
          `;
            })
            .join('')}
        </div>
      </div>
    `)
      .join('');

    // Generate budget summary page
    const categoryBreakdown: Record<string, number> = {};
    days.forEach(day => {
      (day.nodes || []).forEach(node => {
        const nodeType = node.type as string;
        const category = nodeType === 'accommodation' || nodeType === 'hotel' ? 'Accommodation' :
                        nodeType === 'transport' || nodeType === 'transit' ? 'Transportation' :
                        nodeType === 'meal' ? 'Food & Dining' :
                        'Activities';
        const cost = node.cost?.amount || 0;
        categoryBreakdown[category] = (categoryBreakdown[category] || 0) + cost;
      });
    });

    const budgetPageHTML = totalCost > 0 ? `
      <div class="budget-page" style="page-break-before: always; padding: 60px 80px;">
        <h2 style="color: #002B5B; font-size: 36px; font-weight: 800; margin: 0 0 30px 0; border-bottom: 3px solid #F5C542; padding-bottom: 20px;">
          Budget Summary
        </h2>

        <!-- Budget Overview Cards -->
        <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin-bottom: 40px;">
          <div style="padding: 25px; background: linear-gradient(135deg, #002B5B 0%, #004080 100%); border-radius: 12px; color: white;">
            <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px; font-weight: 600;">TOTAL ESTIMATED COST</div>
            <div style="font-size: 36px; font-weight: 800;">${itinerary.currency} ${totalCost.toLocaleString()}</div>
          </div>

          <div style="padding: 25px; background: linear-gradient(135deg, #10B981 0%, #059669 100%); border-radius: 12px; color: white;">
            <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px; font-weight: 600;">BOOKED ACTIVITIES</div>
            <div style="font-size: 36px; font-weight: 800;">${bookedCount}</div>
          </div>

          <div style="padding: 25px; background: linear-gradient(135deg, #F5C542 0%, #E5B532 100%); border-radius: 12px; color: white;">
            <div style="font-size: 14px; opacity: 0.9; margin-bottom: 8px; font-weight: 600;">AVG. DAILY COST</div>
            <div style="font-size: 36px; font-weight: 800;">${itinerary.currency} ${(totalCost / days.length).toFixed(0)}</div>
          </div>
        </div>

        <!-- Category Breakdown -->
        <div style="margin-bottom: 40px;">
          <h3 style="color: #002B5B; font-size: 24px; font-weight: 700; margin: 0 0 20px 0;">Spending by Category</h3>
          <div style="display: flex; flex-direction: column; gap: 15px;">
            ${Object.entries(categoryBreakdown)
              .sort(([, a], [, b]) => b - a)
              .map(([category, amount]) => {
                const percentage = (amount / totalCost) * 100;
                const colors: Record<string, string> = {
                  'Accommodation': '#8B5CF6',
                  'Transportation': '#F5C542',
                  'Food & Dining': '#10B981',
                  'Activities': '#3B82F6',
                };
                const color = colors[category] || '#6B7280';
                
                return `
                  <div style="padding: 20px; background: white; border: 2px solid #e9ecef; border-radius: 12px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
                      <div style="font-size: 16px; font-weight: 600; color: #002B5B;">${category}</div>
                      <div style="text-align: right;">
                        <div style="font-size: 20px; font-weight: 700; color: ${color};">${itinerary.currency} ${amount.toLocaleString()}</div>
                        <div style="font-size: 13px; color: #999;">${percentage.toFixed(1)}% of total</div>
                      </div>
                    </div>
                    <div style="height: 8px; background: #f0f0f0; border-radius: 4px; overflow: hidden;">
                      <div style="height: 100%; background: ${color}; width: ${percentage}%; transition: width 0.3s;"></div>
                    </div>
                  </div>
                `;
              })
              .join('')}
          </div>
        </div>

        <!-- Daily Breakdown -->
        <div>
          <h3 style="color: #002B5B; font-size: 24px; font-weight: 700; margin: 0 0 20px 0;">Daily Spending</h3>
          <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 15px;">
            ${days.map(day => `
              <div style="padding: 20px; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px; text-align: center;">
                <div style="font-size: 14px; color: #666; font-weight: 600; margin-bottom: 8px;">Day ${day.dayNumber}</div>
                <div style="font-size: 24px; font-weight: 700; color: #002B5B;">${itinerary.currency} ${(day.totals?.cost || 0).toLocaleString()}</div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    ` : '';

    return `
      <!DOCTYPE html>
      <html>
        <head>
          <meta charset="UTF-8">
          <title>${destination} Itinerary - EaseMyTrip Planner</title>
          <style>
            @media print {
              body { margin: 0; }
              .no-print { display: none; }
              @page { margin: 0; size: A4; }
            }
            * {
              box-sizing: border-box;
            }
            body {
              font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Helvetica', 'Arial', sans-serif;
              line-height: 1.6;
              color: #333;
              margin: 0;
              padding: 0;
              background: white;
            }
            .print-button {
              position: fixed;
              top: 20px;
              right: 20px;
              z-index: 9999;
              background: #002B5B;
              color: white;
              border: none;
              padding: 12px 24px;
              border-radius: 8px;
              cursor: pointer;
              font-size: 16px;
              font-weight: 600;
              box-shadow: 0 4px 12px rgba(0,43,91,0.3);
              transition: all 0.2s;
            }
            .print-button:hover {
              background: #004080;
              transform: translateY(-2px);
              box-shadow: 0 6px 16px rgba(0,43,91,0.4);
            }
          </style>
        </head>
        <body>
          <button class="print-button no-print" onclick="window.print()">🖨️ Print / Save as PDF</button>
          
          ${coverPageHTML}
          ${budgetPageHTML}
          ${daysHTML}
          
          <!-- Footer on last page -->
          <div style="padding: 60px 80px; text-align: center; color: #999; font-size: 13px;">
            <p style="margin: 0 0 10px 0;">Generated by <strong style="color: #002B5B;">EaseMyTrip Planner</strong> on ${new Date().toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}</p>
            <p style="margin: 0;">Your AI-Powered Travel Companion • Plan smarter, travel better</p>
          </div>
        </body>
      </html>
    `;
  }

  /**
   * Download itinerary as JSON
   */
  async downloadJSON(itinerary: NormalizedItinerary): Promise<void> {
    try {
      const dataStr = JSON.stringify(itinerary, null, 2);
      const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr);
      const exportFileDefaultName = `itinerary-${itinerary.itineraryId}-${Date.now()}.json`;

      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportFileDefaultName);
      linkElement.click();
    } catch (error) {
      console.error('Failed to download JSON:', error);
      throw error;
    }
  }
}

export const exportService = new ExportService();
