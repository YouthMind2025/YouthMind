import React, { useState, useEffect } from 'react';
import PostsTable from '../components/PostsTable';
import PostsFilters from '../components/PostsFilters';
import { usersApi, postsApi } from '../../services/api';
import { mockpostdata } from '../../data/mockData';
import './Posts.css';

// Updated interface to match backend Post entity
interface Post {
  id: string;
  userId: string;
  content: string;
  emotionTag: string;
  aiMetadata?: string;
  createdAt: string;
  user?: {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
  };
}

interface Filters {
  emotionTag: string;
  startDate: string;
  endDate: string;
}

const Posts: React.FC = () => {
  const [posts, setPosts] = useState<Post[]>([]);
  const [filteredPosts, setFilteredPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [filters, setFilters] = useState<Filters>({
    emotionTag: '',
    startDate: '',
    endDate: ''
  });

  const postsPerPage = 10;

  // Fetch all posts - since backend doesn't have GET posts endpoint, 
  // we'll use mock data for now but structure it to match backend format
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        // Note: postsApi.getPosts() is commented out until backend implements the endpoint
        // When backend is ready, uncomment the API call below:
        // const response = await postsApi.getPosts();
        // setPosts(response.data);
        
        // For now, use formatted mock data that matches backend structure
        await new Promise(resolve => setTimeout(resolve, 500)); // Simulate API delay
        
        const formattedMockData = mockpostdata.posts.map(post => ({
          id: post.id,
          userId: post.user.id,
          content: post.content,
          emotionTag: post.emotionTag,
          aiMetadata: undefined,
          createdAt: post.createdAt,
          user: {
            id: post.user.id,
            email: `user${post.user.id}@example.com`,
            firstName: 'User',
            lastName: post.user.id
          }
        }));
        
        setPosts(formattedMockData);
      } catch (error) {
        console.error('Error fetching posts:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchPosts();
  }, []); // Empty dependency array - fetch only once

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
  }, [posts, filters]);

  // Client-side pagination
  const totalPages = Math.ceil(filteredPosts.length / postsPerPage);
  const paginatedPosts = filteredPosts.slice(
    currentPage * postsPerPage,
    (currentPage + 1) * postsPerPage
  );

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