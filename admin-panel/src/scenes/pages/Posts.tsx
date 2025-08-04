import React, { useState, useEffect } from 'react';
import PostsTable from '../components/PostsTable';
import PostsFilters from '../components/PostsFilters';
import { postsApi } from '../../services/api';
import './Posts.css';

//interface to match backend Post entity
interface Post {
  id: string;
  content: string;
  emotionTag: string;
  aiMetadata?: string;
  createdAt: string;
  anonymousUserId?: string;
  understandingCount?: number;
  hasUserUnderstood?: boolean;
}

interface Filters {
  emotionTag: string;
  startDate: string;
  endDate: string;
}

const Posts: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState<Filters>({
    emotionTag: '',
    startDate: '',
    endDate: ''
  });

  const postsPerPage = 10;

  // Fetch posts from backend feed endpoint
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        const response = await postsApi.getFeed(currentPage, postsPerPage, 'newest', {
          emotionTag: filters.emotionTag || undefined,
          startDate: filters.startDate || undefined,
          endDate: filters.endDate || undefined,
        });
        const feedData = response.data;
        if (feedData && feedData.posts) {
          setPosts(feedData.posts);
          setTotalPages(feedData.totalPages || 0);
        } else {
          setPosts([]);
          setTotalPages(0);
        }
      } catch (error) {
        console.error('Error fetching posts:', error);
        setPosts([]);
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, [currentPage, filters]); // Re-fetch when page or filters change

  /*
  // Apply filters
  useEffect(() => {
    let filtered = [...posts];

    if (filters.emotionTag) {
      filtered = filtered.filter(post => 
        post.emotionTag.toLowerCase() === filters.emotionTag.toLowerCase()
      );
    }

    if (filters.startDate) {
      const startDate = new Date(filters.startDate);
      filtered = filtered.filter(post => 
        new Date(post.createdAt) >= startDate
      );
    }

    if (filters.endDate) {
      const endDate = new Date(filters.endDate);
      filtered = filtered.filter(post => 
        new Date(post.createdAt) <= endDate
      );
    }

    setFilteredPosts(filtered);
    setCurrentPage(0); // Reset to first page when filters change
  }, [posts, filters]);*/

  // Posts are already paginated by backend
  const paginatedPosts = posts;

  // Reset to first page when filters change
  useEffect(() => {
    setCurrentPage(0);
  }, [filters]);

  const handleFiltersChange = (newFilters: Filters) => {
    setFilters(newFilters);
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <h1 className="page-title">All Posts</h1>
      </div>
      <div className="page-content">
        <PostsFilters 
          filters={filters} 
          onFiltersChange={handleFiltersChange} 
        />
        <PostsTable 
          posts={paginatedPosts}
          loading={loading}
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>
    </div>
  );
};

export default Posts; 