/**
 * Realistic Earth Globe using globe.gl
 * High-performance WebGL globe with real Earth textures
 */

import { useEffect, useRef } from 'react';
import Globe from 'globe.gl';

export function InteractiveGlobe() {
  const containerRef = useRef<HTMLDivElement>(null);
  const globeRef = useRef<any>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    // Initialize globe with transparent background
    const globe = Globe()(containerRef.current)
      .backgroundColor('rgba(0,0,0,0)') // KEY: Transparent background
      .globeImageUrl('//unpkg.com/three-globe/example/img/earth-blue-marble.jpg')
      .bumpImageUrl('//unpkg.com/three-globe/example/img/earth-topology.png')
      .showAtmosphere(true)
      .atmosphereColor('#6ba3d4')
      .atmosphereAltitude(0.25);

    globeRef.current = globe;

    // Destinations data
    const destinations = [
      { lat: 48.8566, lng: 2.3522, name: 'Paris', size: 0.8 },
      { lat: 25.2048, lng: 55.2708, name: 'Dubai', size: 0.7 },
      { lat: 35.6762, lng: 139.6503, name: 'Tokyo', size: 0.8 },
      { lat: 40.7128, lng: -74.0060, name: 'New York', size: 0.9 },
      { lat: 51.5074, lng: -0.1278, name: 'London', size: 0.8 },
      { lat: 1.3521, lng: 103.8198, name: 'Singapore', size: 0.7 },
    ];

    // Add destination markers
    globe
      .pointsData(destinations)
      .pointAltitude(0.01)
      .pointRadius('size')
      .pointColor(() => '#58ff49ff')
      .pointLabel('name');

    // Configure controls for mouse interaction
    const controls = globe.controls();
    controls.autoRotate = true;
    controls.autoRotateSpeed = 0.5;
    controls.enableZoom = false;
    controls.enablePan = false;
    controls.enableDamping = true;
    controls.dampingFactor = 0.1;
    controls.rotateSpeed = 0.5;

    // Set initial view
    globe.pointOfView({ lat: 20, lng: 0, altitude: 2.5 }, 0);

    // Handle resize
    const handleResize = () => {
      if (containerRef.current) {
        globe.width(containerRef.current.clientWidth);
        globe.height(containerRef.current.clientHeight);
      }
    };

    window.addEventListener('resize', handleResize);
    handleResize();

    return () => {
      window.removeEventListener('resize', handleResize);
      if (globeRef.current) {
        globeRef.current._destructor();
      }
    };
  }, []);

  return (
    <div
      ref={containerRef}
      className="w-full h-full"
    />
  );
}
