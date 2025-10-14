// Shared type definitions for admin panel

// Interface to match backend Post entity
export interface Post {
  id: string;
  userId: string;
  title: string;
  content: string;
  emotion: string;
  preImg?: string;
  resonance: number;
  view: number;
  collect: number;
  topics?: string;
  aiMetadata?: string;
  flagged: boolean;
  flagReason?: string;
  flaggedAt?: string;
  createdAt: string;
  updatedAt?: string;
}

// Filters for posts
export interface PostFilters {
  emotion: string;
  startDate: string;
  endDate: string;
}

// User interface
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt?: string;
  updatedAt?: string;
  recommendationEngineEnabled?: boolean;
}

// Flagged post interface for moderation
export interface FlaggedPost {
  post_id: string;
  flag_reason: string;
  flagged_at: string;
  ai_confidence: number;
  content_preview: string;
  status: string;
}

// Document parsing response interface
export interface DocumentParseResponse {
  title: string;
  content: string;
  intro: string;
  filename: string;
  fileSize: number;
  images: string[]; // Base64 data URLs
  imageMetadata: Record<string, {
    size: number;
    type: string;
    mimeType: string;
    filename: string;
  }>;
}

// Article interface to match backend Article entity
export interface Article {
  id: string;
  title: string;
  content: string;
  intro?: string;
  coverImage?: string;
  published: boolean;
  createdAt: string;
  updatedAt?: string;
  // Website compatibility fields
  date?: string;
  file?: string;
  cover?: string;
}

// Article creation/update request
export interface ArticleRequest {
  title: string;
  content: string;
  intro?: string;
  coverImage?: string;
  published?: boolean;
}

// API response with pagination
export interface ArticleListResponse {
  articles: Article[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalItems: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
  };
  totalElements: number;
  last: boolean;
}

// Recommendation Performance (from backend RecommendationPerformanceResponse)
export interface RecommendationPerformance {
  avgBlockGenerationTimeMs: number;
  cacheHitRate: number;
  eventProcessingRate: number;
  rVectorUpdateSuccessRate: number;
  consumerHealth: Record<string, boolean>;
  streamMetrics: {
    rvectorUpdatesStreamLength: number;
    blockPrecalcStreamLength: number;
    consumerLagMs: number;
    dlqSize: number;
  };
  performanceTargets: Record<string, boolean>;
  generatedAt: string;
}

// Emotion Distribution (from backend EmotionDistributionResponse)
export interface EmotionDistribution {
  overallEmotionDistribution: Record<string, number>;
  topEmotionCombinations: Array<{
    combination: string;
    userCount: number;
    percentage: number;
  }>;
  emotionTrends: Array<{
    emotion: string;
    averageValue: number;
    trend: string;
    timestamp: string;
  }>;
  userSegments: Record<string, number>;
  generatedAt: string;
}

// R Vector interfaces for recommendation vector management
export interface RVector {
  userId: string;
  version: number;
  weights: Record<string, number>; // e.g., {"happy": 0.25, "sad": 0.15, ...}
  topEmotions: string[]; // ["happy", "surprised", "sad"]
  updatedAt: string;
  totalWeight: number;
}

export interface RVectorHistoryItem {
  vector: number[];
  topEmotions: string[];
  timestamp: string;
  version: number;
  trigger: string;
  confidence: number;
}

export interface RVectorHistory {
  userId: string;
  history: RVectorHistoryItem[];
}

// Recommendation Engine Statistics
export interface RecommendationStats {
  totalUsers: number;
  enabledCount: number;
  disabledCount: number;
  enabledPercentage: number;
  disabledPercentage: number;
}

// User Recommendation Status Response
export interface RecommendationStatus {
  userId: string;
  username: string;
  email: string;
  recommendationEngineEnabled: boolean;
  rVector?: {
    version: number;
    createdAt: string;
    updatedAt: string;
    topEmotions: string[];
  };
  feedStatistics: {
    postsCount: number;
    likesCount: number;
    bookmarksCount: number;
  };
}