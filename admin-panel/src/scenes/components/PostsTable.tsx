import React, { useState } from 'react';
import { postsApi } from '../../services/api';

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

interface PostsTableProps {
  posts: Post[];
  loading: boolean;
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

const PostsTable: React.FC<PostsTableProps> = ({
  posts,
  loading,
  currentPage,
  totalPages,
  onPageChange
}) => {
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const truncateContent = (content: string, maxLength: number = 80) => {
    return content.length > maxLength 
      ? content.substring(0, maxLength) + '...' 
      : content;
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const truncateId = (id: string, maxLength: number = 12) => {
    return id.length > maxLength 
      ? id.substring(0, maxLength) + '...' 
      : id;
  };

  const getUserDisplayName = (post: Post) => {
    if (post.user) {
      return `${post.user.firstName} ${post.user.lastName}`;
    }
    return truncateId(post.userId);
  };

  const getUserIdentifier = (post: Post) => {
    if (post.user) {
      return post.user.email;
    }
    return post.userId;
  };

  const toggleExpanded = (postId: string) => {
    const newExpandedRows = new Set(expandedRows);
    if (newExpandedRows.has(postId)) {
      newExpandedRows.delete(postId);
    } else {
      newExpandedRows.add(postId);
    }
    setExpandedRows(newExpandedRows);
  };

  const isExpanded = (postId: string) => expandedRows.has(postId);

  const handleDelete = async (postId: string) => {
    if (window.confirm('Are you sure you want to delete this post?')) {
      try {
        // Note: postsApi.deletePost() is commented out until backend implements the endpoint
        // When backend is ready, uncomment the API call below:
        // await postsApi.deletePost(postId);
        // alert('Post deleted successfully');
        // window.location.reload();
        
        console.log('Delete post:', postId);
        alert('Delete functionality will be available when backend endpoint is implemented');
      } catch (error) {
        console.error('Error deleting post:', error);
        alert('Error deleting post');
      }
    }
  };

  const handleFlag = async (postId: string) => {
    try {
      // Note: postsApi.flagPost() is commented out until backend implements the endpoint
      // When backend is ready, uncomment the API call below:
      // await postsApi.flagPost(postId);
      // alert('Post flagged successfully');
      // window.location.reload();
      
      console.log('Flag post:', postId);
      alert('Flag functionality will be available when backend endpoint is implemented');
    } catch (error) {
      console.error('Error flagging post:', error);
      alert('Error flagging post');
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (posts.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-8 text-center">
        <p className="text-gray-500 text-lg">No posts found matching your criteria.</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-200 text-xs">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-32">
                ID
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
                User
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Content
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-20">
                Emotion
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
                Created
              </th>
              <th className="px-3 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-32">
                Actions
              </th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {posts.map((post) => (
              <React.Fragment key={post.id}>
                <tr className="hover:bg-gray-50">
                  <td className="px-3 py-2 whitespace-nowrap text-xs font-mono text-gray-900">
                    {truncateId(post.id)}
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap text-xs text-gray-900" title={getUserIdentifier(post)}>
                    {getUserDisplayName(post)}
                  </td>
                  <td className="px-3 py-2 text-xs text-gray-900 max-w-xs">
                    {truncateContent(post.content)}
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap">
                    <span className={`inline-flex px-1.5 py-0.5 text-xs font-semibold rounded-full ${
                      post.emotionTag === 'joy' ? 'bg-green-100 text-green-800' :
                      post.emotionTag === 'sadness' ? 'bg-blue-100 text-blue-800' :
                      post.emotionTag === 'anger' ? 'bg-red-100 text-red-800' :
                      post.emotionTag === 'fear' ? 'bg-purple-100 text-purple-800' :
                      post.emotionTag === 'surprise' ? 'bg-yellow-100 text-yellow-800' :
                      post.emotionTag === 'disgust' ? 'bg-orange-100 text-orange-800' :
                      'bg-gray-100 text-gray-800'
                    }`}>
                      {post.emotionTag}
                    </span>
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap text-xs text-gray-500">
                    {formatDate(post.createdAt)}
                  </td>
                  <td className="px-3 py-2 whitespace-nowrap text-xs font-medium space-x-1">
                    <button
                      onClick={() => toggleExpanded(post.id)}
                      className="text-gray-600 hover:text-gray-900 bg-gray-50 hover:bg-gray-100 px-2 py-1 rounded text-xs transition-colors"
                      title={isExpanded(post.id) ? "Collapse" : "Expand"}
                    >
                      {isExpanded(post.id) ? "^" : "v"}
                    </button>
                    <button
                      onClick={() => handleDelete(post.id)}
                      className="text-red-600 hover:text-red-900 bg-red-50 hover:bg-red-100 px-2 py-1 rounded text-xs transition-colors"
                    >
                      Delete
                    </button>
                    <button
                      onClick={() => handleFlag(post.id)}
                      className="text-yellow-600 hover:text-yellow-900 bg-yellow-50 hover:bg-yellow-100 px-2 py-1 rounded text-xs transition-colors"
                    >
                      Flag
                    </button>
                  </td>
                </tr>
                {isExpanded(post.id) && (
                  <tr className="bg-gray-50">
                    <td colSpan={6} className="px-3 py-3 text-xs">
                      <div className="space-y-2">
                        <div>
                          <span className="font-semibold text-gray-700">Content:</span>
                          <p className="mt-1 text-gray-900 whitespace-pre-wrap">{post.content}</p>
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">User ID:</span>
                          <p className="mt-1 font-mono text-gray-900">{post.userId}</p>
                        </div>
                        {post.user && (
                          <div>
                            <span className="font-semibold text-gray-700">User Email:</span>
                            <p className="mt-1 text-gray-900">{post.user.email}</p>
                          </div>
                        )}
                        {post.aiMetadata && (
                          <div>
                            <span className="font-semibold text-gray-700">AI Metadata:</span>
                            <p className="mt-1 text-gray-900">{post.aiMetadata}</p>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>
      
      {/* Pagination */}
      {totalPages > 1 && (
        <div className="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6">
          <div className="flex-1 flex justify-between sm:hidden">
            <button
              onClick={() => onPageChange(Math.max(0, currentPage - 1))}
              disabled={currentPage === 0}
              className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            <button
              onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
              disabled={currentPage === totalPages - 1}
              className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </div>
          <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
            <div>
              <p className="text-sm text-gray-700">
                Showing <span className="font-medium">{currentPage * 10 + 1}</span> to{' '}
                <span className="font-medium">
                  {Math.min((currentPage + 1) * 10, posts.length)}
                </span>{' '}
                of <span className="font-medium">{posts.length}</span> results
              </p>
            </div>
            <div>
              <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                <button
                  onClick={() => onPageChange(Math.max(0, currentPage - 1))}
                  disabled={currentPage === 0}
                  className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    onClick={() => onPageChange(i)}
                    className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                      i === currentPage
                        ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                        : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                    }`}
                  >
                    {i + 1}
                  </button>
                ))}
                <button
                  onClick={() => onPageChange(Math.min(totalPages - 1, currentPage + 1))}
                  disabled={currentPage === totalPages - 1}
                  className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Next
                </button>
              </nav>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PostsTable; 