import React from 'react';
import './FlaggedPosts.css';
import FlaggedPostsTable from '../components/FlaggedPostsTable';

const FlaggedPosts: React.FC = () => {
  return (
    <div className="page-container flagged-bg">
      <div className="page-header">
        <h1 className="page-title">Flagged Posts</h1>
      </div>
      <div className="page-content">
        <FlaggedPostsTable />
      </div>
    </div>
  );
};

export default FlaggedPosts; 