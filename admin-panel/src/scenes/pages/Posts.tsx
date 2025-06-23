import React, { useState, useEffect } from 'react';
import PostsTable from '../components/PostsTable';
import PostsFilters from '../components/PostsFilters';
import { mockpostdata } from '../../data/mockData';
import './Posts.css';

interface Post {
  id: string;
  user: {
    id: string;
    emailHash: string;
  };
  content: string;
  emotionTag: string;
  createdAt: string;
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

  // Fetch all posts once (client-side pagination approach)
  useEffect(() => {
    const fetchPosts = async () => {
      setLoading(true);
      try {
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));
        
        // For now, use mock data. Later this will be replaced with actual API call
        // const response = await postsApi.getPosts();
        // setPosts(response.data); // response.data is the array of posts
        
        setPosts(mockpostdata.posts);
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