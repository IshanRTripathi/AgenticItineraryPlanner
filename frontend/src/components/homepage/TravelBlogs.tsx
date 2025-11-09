/**
 * Travel Blogs Section
 * Task 15: Mobile-optimized with responsive grid and touch-friendly cards
 */

import { Clock, Calendar, ArrowRight } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { mockBlogs } from '../../data/mockBlogs';
import { useMediaQuery } from '@/hooks/useMediaQuery';
import { useTranslation } from '@/i18n';

export function TravelBlogs() {
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width: 768px)');

  return (
    <section className="py-6 sm:py-10 md:py-12 bg-gray-50">
      <div className="container mx-auto">
        {/* Section Header */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 sm:gap-4 mb-4 sm:mb-8 px-4">
          <div className="flex-1">
            <h2 className="text-xl sm:text-3xl font-bold text-gray-900">{t('pages.home.travelBlogs.title')}</h2>
            <p className="text-xs sm:text-base text-gray-600 mt-1">{t('pages.home.travelBlogs.subtitle')}</p>
          </div>
          <button className="hidden md:flex items-center text-primary hover:text-primary-hover font-medium transition-colors min-h-[44px] touch-manipulation">
            {t('pages.home.travelBlogs.viewAll')}
            <ArrowRight className="ml-2 h-4 w-4" />
          </button>
        </div>

        {/* Blog Grid - Horizontal scroll on mobile, Grid on desktop */}
        <div className={`
          ${isMobile 
            ? 'flex overflow-x-auto gap-2.5 pb-4 scrollbar-hide snap-x snap-mandatory px-4' 
            : 'grid grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5 md:gap-6 px-4'
          }
        `}>
          {mockBlogs.map((blog) => (
            <div key={blog.id} className={isMobile ? 'flex-shrink-0 w-[220px] snap-start' : ''}>
              <BlogCard blog={blog} />
            </div>
          ))}
        </div>

        {/* Mobile View All Button */}
        <div className="mt-4 sm:mt-8 md:hidden px-4">
          <button className="w-full flex items-center justify-center text-primary hover:text-primary-hover font-medium transition-colors py-3 min-h-[48px] border border-primary rounded-lg touch-manipulation active:scale-95">
            {t('pages.home.travelBlogs.viewAll')}
            <ArrowRight className="ml-2 h-4 w-4" />
          </button>
        </div>
      </div>
    </section>
  );
}

interface BlogCardProps {
  blog: {
    id: string;
    title: string;
    excerpt: string;
    category: string;
    imageUrl: string;
    readTime: number;
    publishDate: string;
    author: string;
  };
}

function BlogCard({ blog }: BlogCardProps) {
  const { t } = useTranslation();
  return (
    <Card className="overflow-hidden hover:shadow-lg transition-all duration-300 hover:-translate-y-1 cursor-pointer group p-0 touch-manipulation active:scale-[0.98] h-full">
      {/* Image */}
      <div className="relative aspect-video overflow-hidden">
        <img
          src={blog.imageUrl}
          alt={blog.title}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
          loading="lazy"
        />
        {/* Category Badge */}
        <div className="absolute top-2 left-2">
          <Badge className="bg-white text-gray-900 hover:bg-white text-[10px] sm:text-xs px-2 py-0.5">
            {blog.category}
          </Badge>
        </div>
      </div>

      {/* Content */}
      <div className="p-2 sm:p-4">
        {/* Title */}
        <h3 className="text-xs sm:text-lg font-semibold text-gray-900 mb-1 sm:mb-2 line-clamp-2 group-hover:text-primary transition-colors">
          {blog.title}
        </h3>

        {/* Excerpt */}
        <p className="text-[9px] sm:text-sm text-gray-600 mb-1.5 sm:mb-4 line-clamp-2 sm:line-clamp-3">
          {blog.excerpt}
        </p>

        {/* Meta Information */}
        <div className="flex items-center gap-2 sm:gap-4 text-[8px] sm:text-xs text-gray-500 pt-1.5 sm:pt-4 border-t">
          <div className="flex items-center gap-0.5">
            <Clock className="h-2 w-2 sm:h-3.5 sm:w-3.5" />
            <span>{blog.readTime}m</span>
          </div>
          <div className="flex items-center gap-0.5">
            <Calendar className="h-2 w-2 sm:h-3.5 sm:w-3.5" />
            <span>{blog.publishDate.split(' ')[0]}</span>
          </div>
        </div>

        {/* Author */}
        <div className="mt-1 sm:mt-3 text-[8px] sm:text-xs text-gray-500 truncate">
          {t('pages.home.travelBlogs.by')} <span className="font-medium text-gray-700">{blog.author}</span>
        </div>

        {/* Read More Link */}
        <button className="mt-1.5 sm:mt-4 text-[9px] sm:text-sm font-medium text-primary hover:text-primary-hover flex items-center gap-0.5 transition-colors min-h-[32px] sm:min-h-[44px] touch-manipulation">
          {t('pages.home.travelBlogs.readMore')}
          <ArrowRight className="h-2 w-2 sm:h-3.5 sm:w-3.5" />
        </button>
      </div>
    </Card>
  );
}
