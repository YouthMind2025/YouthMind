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
