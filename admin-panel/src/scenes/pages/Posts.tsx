import React, { useState, useEffect } from 'react';
import PostsTable from '../components/PostsTable';
import PostsFilters from '../components/PostsFilters';
import { postsApi } from '../../services/api';
import { Post, PostFilters } from '../../types';
import './Posts.css';

const Posts: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filters, setFilters] = useState<PostFilters>({
    emotion: '',
    startDate: '',
    endDate: ''
  });

  const postsPerPage = 10;

  // Fetch posts from backend endpoint
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        console.log('Fetching posts with params:', {
          page: currentPage + 1,
          limit: postsPerPage,
          emotion: filters.emotion || undefined,
          sort: 'newest'
        });
        
        const response = await postsApi.getPosts(
          currentPage + 1, // Will be converted to 0-based in API call
          postsPerPage,
          filters.emotion || undefined,
          'newest'
        );
        
        console.log('Raw API response:', response);
        console.log('Response data:', response.data);
        
        const data = response.data;
        
        // Handle different possible response structures
        if (data && data.success && data.data) {
          // Backend returns ApiResponse<FeedResponse> format
          const feedData = data.data;
          console.log('Feed data:', feedData);
          
          if (feedData.posts && Array.isArray(feedData.posts)) {
            console.log('Found posts in FeedResponse format:', feedData.posts.length);
            setPosts(feedData.posts);
            // Use pagination info from FeedResponse if available
            if (feedData.pagination) {
              setTotalPages(feedData.pagination.totalPages || 1);
            } else {
              setTotalPages(Math.ceil(feedData.posts.length / postsPerPage) + 1);
            }
          } else if (Array.isArray(feedData)) {
            // Fallback: direct array in data
            console.log('Found posts as direct array in data:', feedData.length);
            setPosts(feedData);
            setTotalPages(Math.ceil(feedData.length / postsPerPage) + 1);
          } else {
            console.log('No posts found in feed data:', feedData);
            setPosts([]);
            setTotalPages(0);
          }
        } else if (Array.isArray(data)) {
          // Direct array response (fallback)
          console.log('Found posts in direct array format:', data.length);
          setPosts(data);
          setTotalPages(Math.ceil(data.length / postsPerPage) + 1);
        } else {
          console.log('No posts found or unexpected format:', data);
          setPosts([]);
          setTotalPages(0);
        }
      } catch (error: any) {
        console.error('Error fetching posts:', error);
        console.error('Error details:', error.response?.data);
        setPosts([]);
        setTotalPages(0);
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

  const handleFiltersChange = (newFilters: PostFilters) => {
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