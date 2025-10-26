import { Clock, Calendar, ArrowRight } from 'lucide-react';
import { Card, CardContent } from '../ui/card';
import { Badge } from '../ui/badge';
import { mockBlogs } from '../../data/mockBlogs';

export function TravelBlogs() {
  return (
    <section className="py-16 bg-gray-50">
      <div className="container mx-auto px-4">
        {/* Section Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-3xl font-bold text-gray-900">Travel Inspiration</h2>
            <p className="text-gray-600 mt-2">Discover stories and tips from fellow travelers</p>
          </div>
          <button className="hidden md:flex items-center text-primary hover:text-primary-hover font-medium transition-colors">
            View All Articles
            <ArrowRight className="ml-2 h-4 w-4" />
          </button>
        </div>

        {/* Blog Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {mockBlogs.map((blog) => (
            <BlogCard key={blog.id} blog={blog} />
          ))}
        </div>

        {/* Mobile View All Button */}
        <div className="mt-8 md:hidden">
          <button className="w-full flex items-center justify-center text-primary hover:text-primary-hover font-medium transition-colors py-3 border border-primary rounded-lg">
            View All Articles
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
  return (
    <Card className="overflow-hidden hover:shadow-lg transition-all duration-300 hover:-translate-y-1 cursor-pointer group">
      {/* Image */}
      <div className="relative aspect-video overflow-hidden">
        <img
          src={blog.imageUrl}
          alt={blog.title}
          className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
        />
        {/* Category Badge */}
        <div className="absolute top-4 left-4">
          <Badge className="bg-white text-gray-900 hover:bg-white">
            {blog.category}
          </Badge>
        </div>
      </div>

      {/* Content */}
      <CardContent className="p-5">
        {/* Title */}
        <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-2 group-hover:text-primary transition-colors">
          {blog.title}
        </h3>

        {/* Excerpt */}
        <p className="text-sm text-gray-600 mb-4 line-clamp-3">
          {blog.excerpt}
        </p>

        {/* Meta Information */}
        <div className="flex items-center justify-between text-xs text-gray-500 pt-4 border-t">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              <span>{blog.readTime} min read</span>
            </div>
            <div className="flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" />
              <span>{blog.publishDate}</span>
            </div>
          </div>
        </div>

        {/* Author */}
        <div className="mt-3 text-xs text-gray-500">
          By <span className="font-medium text-gray-700">{blog.author}</span>
        </div>

        {/* Read More Link */}
        <button className="mt-4 text-sm font-medium text-primary hover:text-primary-hover flex items-center gap-1 transition-colors">
          Read More
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      </CardContent>
    </Card>
  );
}
