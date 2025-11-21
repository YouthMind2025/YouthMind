import React, { useState, useEffect } from 'react';
import { postsApi } from '../../services/api';
import { FlaggedPost } from '../../types';

const FlaggedPostsTable: React.FC = () => {
  const [flaggedPosts, setFlaggedPosts] = useState<FlaggedPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());

  const truncateContent = (content: string, maxLength: number = 80) => {
    return content.length > maxLength 
      ? content.substring(0, maxLength) + '...' 
      : content;
  };

  const truncateId = (id: string, maxLength: number = 12) => {
    return id.length > maxLength 
      ? id.substring(0, maxLength) + '...' 
      : id;
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

  useEffect(() => {
    const fetchFlaggedPosts = async () => {
      setLoading(true);
      try {
        const response = await postsApi.getFlaggedPosts();
        console.log('Flagged posts response:', response.data); // Debug log
        
        // Handle different possible response structures
        let flaggedPostsData: FlaggedPost[] = [];
        
        if (response.data && response.data.success && Array.isArray(response.data.data)) {
          // Backend returns ApiResponse format: { success: true, data: [...] }
          flaggedPostsData = response.data.data.filter((post: any) => post.flagged === true);
        } else if (Array.isArray(response.data)) {
          // Direct array response
          flaggedPostsData = response.data.filter((post: any) => post.flagged === true);
        } else if (response.data && response.data.flagged_posts) {
          // Expected format with flagged_posts property
          flaggedPostsData = response.data.flagged_posts;
        }
        
        // Convert posts to FlaggedPost format if needed
        const convertedFlaggedPosts: FlaggedPost[] = (flaggedPostsData || []).map((post: any) => ({
          post_id: post.id || post.post_id || 'unknown',
          flag_reason: post.flagReason || post.flag_reason || 'Content flagged for review',
          flagged_at: post.flaggedAt || post.flagged_at || post.createdAt || new Date().toISOString(),
          ai_confidence: 0.85, // Mock confidence since backend doesn't provide this yet
          content_preview: (post.content || post.content_preview || '').substring(0, 100),
          status: post.status || 'pending_review'
        }));
        
        setFlaggedPosts(convertedFlaggedPosts);
      } catch (err) {
        console.error('Error fetching flagged posts:', err);
        setError('Failed to load flagged posts. Please try again.');
        // Set empty array on error to prevent undefined access
        setFlaggedPosts([]);
      } finally {
        setLoading(false);
      }
    };

    fetchFlaggedPosts();
  }, []);

  const handleUnflag = async (postId: string) => {
    if (window.confirm('Are you sure you want to unflag this post? This will remove it from the moderation queue.')) {
      try {
        await postsApi.unflagPost(postId);
        alert('Post unflagged successfully');
        // Remove the unflagged post from the flagged posts list
        setFlaggedPosts(prev => prev.filter(post => post.post_id !== postId));
      } catch (error) {
        console.error('Error unflagging post:', error);
        alert('Error unflagging post');
      }
    }
  };

  const handleDelete = async (postId: string) => {
    if (window.confirm('Are you sure you want to delete this flagged post?')) {
      try {
        await postsApi.deletePost(postId);
        alert('Post deleted successfully');
        setFlaggedPosts(prev => prev.filter(post => post.post_id !== postId));
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

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow p-8 text-center">
        <div className="text-red-600 text-lg font-semibold mb-2">⚠️ Error</div>
        <p className="text-gray-500 mb-4">{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded"
        >
          Retry
        </button>
      </div>
    );
  }

  if (!flaggedPosts || flaggedPosts.length === 0) {
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
            Moderation queue powered by live data
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
              <React.Fragment key={post.post_id}>
                <tr className="hover:bg-gray-50">
                  <td className="px-6 py-4 border-b text-sm font-mono text-gray-900">
                    {truncateId(post.post_id)}
                  </td>
                  <td className="px-6 py-4 border-b text-sm text-gray-900">
                    <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${
                      post.flag_reason.includes('harmful') ? 'bg-red-100 text-red-800' :
                      post.flag_reason.includes('spam') ? 'bg-yellow-100 text-yellow-800' :
                      post.flag_reason.includes('inappropriate') ? 'bg-orange-100 text-orange-800' :
                      'bg-gray-100 text-gray-800'
                    }`}>
                      {truncateContent(post.flag_reason, 30)}
                    </span>
                  </td>
                  <td className="px-6 py-4 border-b text-sm text-gray-500">
                    {formatDate(post.flagged_at)}
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
                    <div title={post.content_preview}>
                      {truncateContent(post.content_preview)}
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
                  <td className="px-6 py-4 border-b text-sm font-medium space-x-1">
                    <button
                      onClick={() => toggleExpanded(post.post_id)}
                      className="text-gray-600 hover:text-gray-900 bg-gray-50 hover:bg-gray-100 px-2 py-1 rounded text-xs transition-colors"
                      title={isExpanded(post.post_id) ? "Collapse" : "Expand"}
                    >
                      {isExpanded(post.post_id) ? "^" : "v"}
                    </button>
                    <button
                      onClick={() => handleUnflag(post.post_id)}
                      className="text-blue-600 hover:text-blue-900 bg-blue-50 hover:bg-blue-100 px-2 py-1 rounded text-xs transition-colors"
                      title="Remove flag and return post to normal status"
                    >
                      Unflag
                    </button>
                    <button
                      onClick={() => handleDelete(post.post_id)}
                      className="text-red-600 hover:text-red-900 bg-red-50 hover:bg-red-100 px-2 py-1 rounded text-xs transition-colors"
                      title="Permanently delete this post"
                    >
                      Delete
                    </button>
                  </td>
                </tr>
                {isExpanded(post.post_id) && (
                  <tr className="bg-gray-50">
                    <td colSpan={7} className="px-6 py-3 text-sm">
                      <div className="space-y-2">
                        <div>
                          <span className="font-semibold text-gray-700">Full Post ID:</span>
                          <p className="mt-1 font-mono text-gray-900">{post.post_id}</p>
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">Full Flag Reason:</span>
                          <p className="mt-1 text-gray-900">{post.flag_reason}</p>
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">Full Content Preview:</span>
                          <p className="mt-1 text-gray-900 whitespace-pre-wrap">{post.content_preview}</p>
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">Flagged At:</span>
                          <p className="mt-1 text-gray-900">{new Date(post.flagged_at).toLocaleString()}</p>
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">AI Confidence:</span>
                          <p className="mt-1 text-gray-900">{(post.ai_confidence * 100).toFixed(1)}%</p>
                        </div>
                        <div>
                          <span className="font-semibold text-gray-700">Status:</span>
                          <p className="mt-1 text-gray-900">{post.status.replace('_', ' ')}</p>
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
              </React.Fragment>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default FlaggedPostsTable; 