import React from 'react';
import { flagedmockData } from '../../data/mockData';

const FlaggedPostsTable: React.FC = () => {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full bg-white border border-gray-300">
        <thead>
          <tr className="bg-gray-100">
            <th className="px-6 py-3 border-b text-left">Post ID</th>
            <th className="px-6 py-3 border-b text-left">Flag Reason</th>
            <th className="px-6 py-3 border-b text-left">Flagged At</th>
            <th className="px-6 py-3 border-b text-left">AI Confidence</th>
            <th className="px-6 py-3 border-b text-left">Content Preview</th>
            <th className="px-6 py-3 border-b text-left">Status</th>
          </tr>
        </thead>
        <tbody>
          {flagedmockData.flagged_posts.map((post) => (
            <tr key={post.post_id} className="hover:bg-gray-50">
              <td className="px-6 py-4 border-b">{post.post_id}</td>
              <td className="px-6 py-4 border-b">{post.flag_reason}</td>
              <td className="px-6 py-4 border-b">{new Date(post.flagged_at).toLocaleString()}</td>
              <td className="px-6 py-4 border-b">{(post.ai_confidence * 100).toFixed(1)}%</td>
              <td className="px-6 py-4 border-b">{post.content_preview}</td>
              <td className="px-6 py-4 border-b">
                <span className={`px-2 py-1 rounded-full text-sm ${
                  post.status === 'pending_review' ? 'bg-yellow-100 text-yellow-800' : 'bg-green-100 text-green-800'
                }`}>
                  {post.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default FlaggedPostsTable; 