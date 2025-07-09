import React, { useState, useEffect } from 'react';
import { postsApi } from '../../services/api';
import { flagedmockData } from '../../data/mockData';

interface FlaggedPost {
  post_id: string;
  flag_reason: string;
  flagged_at: string;
  ai_confidence: number;
  content_preview: string;
  status: string;
}

const FlaggedPostsTable: React.FC = () => {
  const [flaggedPosts, setFlaggedPosts] = useState<FlaggedPost[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchFlaggedPosts = async () => {
      setLoading(true);
      try {
        // Note: Backend doesn't have flagged posts endpoint
        // For now, use mock data but in a real system this would be:
        // const response = await postsApi.getFlaggedPosts();
        console.log('Backend does not have flagged posts endpoint. Using mock data.');
        
        // Simulate API delay
        await new Promise(resolve => setTimeout(resolve, 500));
        setFlaggedPosts(flagedmockData.flagged_posts);
      } catch (error) {
        console.error('Error fetching flagged posts:', error);
        setFlaggedPosts(flagedmockData.flagged_posts);
      } finally {
        setLoading(false);
      }
    };

    fetchFlaggedPosts();
  }, []);

  const handleResolve = async (postId: string) => {
    if (window.confirm('Mark this flagged post as resolved?')) {
      try {
        // In a real system, this would call an API endpoint
        console.log('Resolving flagged post:', postId);
        alert('Post marked as resolved (mock operation)');
        // Update local state to remove the resolved post
        setFlaggedPosts(prev => prev.filter(post => post.post_id !== postId));
      } catch (error) {
        console.error('Error resolving flagged post:', error);
        alert('Error resolving post');
      }
    }
  };

  const handleDelete = async (postId: string) => {
    if (window.confirm('Are you sure you want to delete this flagged post?')) {
      try {
        // Note: postsApi.deletePost() is commented out until backend implements the endpoint
        // When backend is ready, uncomment the API call below:
        // await postsApi.deletePost(postId);
        // alert('Post deleted successfully');
        // setFlaggedPosts(prev => prev.filter(post => post.post_id !== postId));
        
        console.log('Delete flagged post:', postId);
        alert('Delete functionality will be available when backend endpoint is implemented');
      } catch (error) {
        console.error('Error deleting post:', error);
        alert('Error deleting post');
      }
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
        <span className="ml-3 text-gray-600">Loading flagged posts...</span>
      </div>
    );
  }

  if (flaggedPosts.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow p-8 text-center">
        <div className="text-green-600 text-lg font-semibold mb-2">🎉 All Clear!</div>
        <p className="text-gray-500">No flagged posts found. Great job keeping the community safe!</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-medium text-gray-900">
            Flagged Posts ({flaggedPosts.length})
          </h3>
          <span className="text-sm text-gray-500">
            Note: Using mock data as backend doesn't have flagged posts endpoints
          </span>
        </div>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full bg-white border border-gray-300">
          <thead>
            <tr className="bg-gray-100">
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Post ID</th>
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Flag Reason</th>
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Flagged At</th>
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">AI Confidence</th>
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Content Preview</th>
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
              <th className="px-6 py-3 border-b text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody>
            {flaggedPosts.map((post) => (
              <tr key={post.post_id} className="hover:bg-gray-50">
                <td className="px-6 py-4 border-b text-sm font-mono text-gray-900">
                  {post.post_id.length > 12 ? post.post_id.substring(0, 12) + '...' : post.post_id}
                </td>
                <td className="px-6 py-4 border-b text-sm text-gray-900">
                  <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                    post.flag_reason.includes('harmful') ? 'bg-red-100 text-red-800' :
                    post.flag_reason.includes('spam') ? 'bg-yellow-100 text-yellow-800' :
                    post.flag_reason.includes('inappropriate') ? 'bg-orange-100 text-orange-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {post.flag_reason}
                  </span>
                </td>
                <td className="px-6 py-4 border-b text-sm text-gray-500">
                  {new Date(post.flagged_at).toLocaleString()}
                </td>
                <td className="px-6 py-4 border-b text-sm text-gray-900">
                  <div className="flex items-center">
                    <span className="mr-2">{(post.ai_confidence * 100).toFixed(1)}%</span>
                    <div className="w-16 bg-gray-200 rounded-full h-2">
                      <div 
                        className={`h-2 rounded-full ${
                          post.ai_confidence > 0.8 ? 'bg-red-500' :
                          post.ai_confidence > 0.6 ? 'bg-yellow-500' :
                          'bg-green-500'
                        }`}
                        style={{ width: `${post.ai_confidence * 100}%` }}
                      ></div>
                    </div>
                  </div>
                </td>
                <td className="px-6 py-4 border-b text-sm text-gray-900 max-w-xs">
                  <div className="truncate" title={post.content_preview}>
                    {post.content_preview}
                  </div>
                </td>
                <td className="px-6 py-4 border-b text-sm">
                  <span className={`px-2 py-1 rounded-full text-sm ${
                    post.status === 'pending_review' ? 'bg-yellow-100 text-yellow-800' : 
                    post.status === 'resolved' ? 'bg-green-100 text-green-800' :
                    'bg-gray-100 text-gray-800'
                  }`}>
                    {post.status.replace('_', ' ')}
                  </span>
                </td>
                <td className="px-6 py-4 border-b text-sm font-medium space-x-2">
                  <button
                    onClick={() => handleResolve(post.post_id)}
                    className="text-green-600 hover:text-green-900 bg-green-50 hover:bg-green-100 px-3 py-1 rounded text-xs transition-colors"
                  >
                    Resolve
                  </button>
                  <button
                    onClick={() => handleDelete(post.post_id)}
                    className="text-red-600 hover:text-red-900 bg-red-50 hover:bg-red-100 px-3 py-1 rounded text-xs transition-colors"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default FlaggedPostsTable; 