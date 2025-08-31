import React from 'react';
import { PostFilters } from '../../types';

interface PostsFiltersProps {
  filters: PostFilters;
  onFiltersChange: (filters: PostFilters) => void;
}

const PostsFilters: React.FC<PostsFiltersProps> = ({ filters, onFiltersChange }) => {
  const emotions = ['happy', 'sad', 'angry', 'fear', 'surprised', 'disgusted', 'conflicted'];

  const handleFilterChange = (key: keyof PostFilters, value: string) => {
    onFiltersChange({
      ...filters,
      [key]: value
    });
  };

  const clearFilters = () => {
    onFiltersChange({
      emotion: '',
      startDate: '',
      endDate: ''
    });
  };

  return (
    <div className="bg-white rounded-lg shadow p-4 mb-4">
      <div className="flex justify-between items-center mb-3">
        <h3 className="text-base font-medium text-gray-900">Filters</h3>
        <button
          onClick={clearFilters}
          className="text-sm text-gray-500 hover:text-gray-700 underline"
        >
          Clear all filters
        </button>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div>
          <label htmlFor="emotion" className="block text-sm font-medium text-gray-700 mb-1">
            Emotion
          </label>
          <select
            id="emotion"
            value={filters.emotion}
            onChange={(e) => handleFilterChange('emotion', e.target.value)}
            className="block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 text-sm"
          >
            <option value="">All Emotions</option>
            {emotions.map(emotion => (
              <option key={emotion} value={emotion}>
                {emotion.charAt(0).toUpperCase() + emotion.slice(1)}
              </option>
            ))}
          </select>
        </div>
        
        <div>
          <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
            Start Date
          </label>
          <input
            type="datetime-local"
            id="startDate"
            value={filters.startDate}
            onChange={(e) => handleFilterChange('startDate', e.target.value)}
            className="block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 text-sm"
          />
        </div>
        
        <div>
          <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
            End Date
          </label>
          <input
            type="datetime-local"
            id="endDate"
            value={filters.endDate}
            onChange={(e) => handleFilterChange('endDate', e.target.value)}
            className="block w-full border-gray-300 rounded-md shadow-sm focus:ring-indigo-500 focus:border-indigo-500 text-sm"
          />
        </div>
      </div>
    </div>
  );
};

export default PostsFilters; 