// TypeScript Type Definitions for YouthMind Frontend Integration

// ==================== ENUMS ====================

export type EmotionType = 
  | 'HAPPY' | 'SAD' | 'ANGRY' | 'ANXIOUS' | 'EXCITED' | 'CALM'
  | 'FRUSTRATED' | 'HOPEFUL' | 'LONELY' | 'GRATEFUL' | 'CONFUSED'
  | 'OVERWHELMED' | 'CONTENT' | 'WORRIED' | 'PROUD' | 'DISAPPOINTED';

export type EngagementType = 'RESONANCE' | 'COLLECT' | 'VIEW';

export type CommentEngagementType = 'LIKE';

export type Theme = 'LIGHT' | 'DARK' | 'SYSTEM';

export type PrivacyLevel = 'PUBLIC' | 'FRIENDS' | 'PRIVATE';

export type MessageSender = 'user' | 'ai';

// ==================== BASE ENTITIES ====================

export interface User {
  id: string;
  email: string;
  username?: string;
  firstName: string;
  lastName: string;
  bio?: string;
  avatar?: string;
  postsCount: number;
  likesCount: number;
  bookmarksCount: number;
  version: number;
  lastLoginAt?: string;
  loginCount: number;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

export interface UserPreferences {
  id: string;
  userId: string;
  theme: Theme;
  language: string;
  notificationsEnabled: boolean;
  emailNotifications: boolean;
  pushNotifications: boolean;
  privacyLevel: PrivacyLevel;
  aiSuggestionsEnabled: boolean;
  anonymousPostingAllowed: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Post {
  id: string;
  userId: string;
  title: string;
  content: string;
  emotion: EmotionType;
  topics?: string;
  preImg?: string;
  likes: number;
  bookmarks: number;
  views: number;
  resonanceCount: number;
  collectCount: number;
  aiMetadata?: string;
  flagged: boolean;
  flagReason?: string;
  flaggedAt?: string;
  isAnonymous: boolean;
  version: number;
  lastModifiedBy?: string;
  deletedAt?: string;
  createdAt: string;
  updatedAt: string;
  
  // Computed fields (from joins or aggregations)
  author?: User;
  isLiked?: boolean;
  isBookmarked?: boolean;
  isResonated?: boolean;
  isCollected?: boolean;
  commentsCount?: number;
}

export interface Comment {
  id: string;
  postId: string;
  authorId: string;
  content: string;
  likes: number;
  parentId?: string;
  deletedAt?: string;
  createdAt: string;
  
  // Computed fields
  author?: User;
  isLiked?: boolean;
  replies?: Comment[];
  repliesCount?: number;
}

export interface UserEngagement {
  id: string;
  userId: string;
  postId: string;
  type: EngagementType;
  createdAt: string;
}

export interface CommentEngagement {
  id: string;
  userId: string;
  commentId: string;
  type: CommentEngagementType;
  createdAt: string;
}

export interface Conversation {
  id: string;
  userId: string;
  title: string;
  isPinned: boolean;
  createdAt: string;
  updatedAt: string;
  
  // Computed fields
  messagesCount?: number;
  lastMessage?: Message;
}

export interface Message {
  id: string;
  conversationId: string;
  text: string;
  sender: MessageSender;
  timestamp: string;
}

export interface Draft {
  id: string;
  userId: string;
  title: string;
  content: string;
  emotion?: string;
  topics?: string;
  createdAt: string;
  updatedAt: string;
  deletedAt?: string;
}

export interface AuditLog {
  id: string;
  entityType: string;
  entityId: string;
  action: 'CREATE' | 'UPDATE' | 'DELETE' | 'RESTORE';
  userId?: string;
  oldValues?: string;
  newValues?: string;
  ipAddress?: string;
  userAgent?: string;
  createdAt: string;
}

// ==================== REQUEST TYPES ====================

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  username?: string;
}

export interface CreatePostRequest {
  title: string;
  content: string;
  emotion: EmotionType;
  topics?: string;
  preImg?: string;
  isAnonymous?: boolean;
}

export interface UpdatePostRequest {
  title?: string;
  content?: string;
  emotion?: EmotionType;
  topics?: string;
  preImg?: string;
}

export interface CreateCommentRequest {
  postId: string;
  content: string;
  parentId?: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface EngagementRequest {
  type: EngagementType;
}

export interface CommentEngagementRequest {
  type: CommentEngagementType;
}

export interface CreateConversationRequest {
  title: string;
  initialMessage?: string;
}

export interface SendMessageRequest {
  text: string;
}

export interface CreateDraftRequest {
  title: string;
  content: string;
  emotion?: string;
  topics?: string;
}

export interface UpdateDraftRequest {
  title?: string;
  content?: string;
  emotion?: string;
  topics?: string;
}

export interface UpdateUserProfileRequest {
  firstName?: string;
  lastName?: string;
  username?: string;
  bio?: string;
  avatar?: string;
}

export interface UpdateUserPreferencesRequest {
  theme?: Theme;
  language?: string;
  notificationsEnabled?: boolean;
  emailNotifications?: boolean;
  pushNotifications?: boolean;
  privacyLevel?: PrivacyLevel;
  aiSuggestionsEnabled?: boolean;
  anonymousPostingAllowed?: boolean;
}

// ==================== RESPONSE TYPES ====================

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface PostCreateResponse {
  post: Post;
  message: string;
}

export interface PostEngagementResponse {
  success: boolean;
  newCount: number;
  userEngaged: boolean;
}

export interface CommentCreateResponse {
  comment: Comment;
  message: string;
}

export interface FeedResponse {
  posts: Post[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface SearchResponse {
  posts: Post[];
  users: User[];
  totalResults: number;
  query: string;
  suggestions?: string[];
}

export interface TrendingResponse {
  posts: Post[];
  timeframe: string;
  totalCount: number;
}

export interface ConversationListResponse {
  conversations: Conversation[];
  totalCount: number;
}

export interface MessageListResponse {
  messages: Message[];
  conversationId: string;
  totalCount: number;
}

export interface DraftListResponse {
  drafts: Draft[];
  totalCount: number;
}

export interface UserPostsResponse {
  posts: Post[];
  user: User;
  totalCount: number;
}

export interface AnalyticsResponse {
  userStats: {
    totalPosts: number;
    totalLikes: number;
    totalViews: number;
    engagementRate: number;
  };
  emotionDistribution: {
    emotion: EmotionType;
    count: number;
    percentage: number;
  }[];
  activityTimeline: {
    date: string;
    posts: number;
    engagements: number;
  }[];
}

// ==================== UTILITY TYPES ====================

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'ASC' | 'DESC';
}

export interface SearchParams extends PaginationParams {
  query: string;
  emotion?: EmotionType;
  userId?: string;
  dateFrom?: string;
  dateTo?: string;
}

export interface FeedParams extends PaginationParams {
  emotion?: EmotionType;
  userId?: string;
  following?: boolean;
}

export interface TrendingParams extends PaginationParams {
  timeframe?: 'day' | 'week' | 'month' | 'year';
  emotion?: EmotionType;
}

// ==================== ERROR TYPES ====================

export interface ApiError extends Error {
  status?: number;
  code?: string;
  data?: any;
}

export interface ValidationError {
  field: string;
  message: string;
  code: string;
}

export interface FormErrors {
  [key: string]: string | ValidationError[];
}

// ==================== WEBSOCKET TYPES ====================

export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: string;
}

export interface PostUpdateMessage extends WebSocketMessage {
  type: 'POST_UPDATE';
  data: {
    postId: string;
    updateType: 'ENGAGEMENT' | 'COMMENT' | 'EDIT' | 'DELETE';
    post?: Post;
    engagement?: UserEngagement;
    comment?: Comment;
  };
}

export interface NotificationMessage extends WebSocketMessage {
  type: 'NOTIFICATION';
  data: {
    id: string;
    userId: string;
    title: string;
    message: string;
    type: 'LIKE' | 'COMMENT' | 'FOLLOW' | 'MENTION' | 'SYSTEM';
    relatedId?: string;
    read: boolean;
  };
}

export interface ChatMessage extends WebSocketMessage {
  type: 'CHAT_MESSAGE';
  data: Message;
}

// ==================== COMPONENT PROP TYPES ====================

export interface PostCardProps {
  post: Post;
  showActions?: boolean;
  compact?: boolean;
  onEngagement?: (postId: string, type: EngagementType) => void;
  onComment?: (postId: string) => void;
  onShare?: (postId: string) => void;
}

export interface CommentItemProps {
  comment: Comment;
  level?: number;
  maxLevel?: number;
  onReply?: (commentId: string, content: string) => void;
  onLike?: (commentId: string) => void;
  onDelete?: (commentId: string) => void;
}

export interface FeedProps {
  emotion?: EmotionType;
  userId?: string;
  showCreatePost?: boolean;
  infiniteScroll?: boolean;
  pageSize?: number;
}

export interface ChatProps {
  conversationId?: string;
  autoStart?: boolean;
  placeholder?: string;
  height?: string;
}

// ==================== HOOK TYPES ====================

export interface UsePostsResult {
  posts: Post[];
  loading: boolean;
  error: ApiError | null;
  hasMore: boolean;
  loadMore: () => void;
  refresh: () => void;
  createPost: (post: CreatePostRequest) => Promise<Post>;
  updatePost: (id: string, post: UpdatePostRequest) => Promise<Post>;
  deletePost: (id: string) => Promise<void>;
  engagePost: (id: string, type: EngagementType) => Promise<void>;
}

export interface UseAuthResult {
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  updateProfile: (data: UpdateUserProfileRequest) => Promise<void>;
  updatePreferences: (data: UpdateUserPreferencesRequest) => Promise<void>;
}

export interface UseChatResult {
  conversations: Conversation[];
  currentConversation: Conversation | null;
  messages: Message[];
  loading: boolean;
  isTyping: boolean;
  createConversation: (data: CreateConversationRequest) => Promise<Conversation>;
  selectConversation: (id: string) => void;
  sendMessage: (text: string) => Promise<void>;
  deleteConversation: (id: string) => Promise<void>;
}

// ==================== FORM TYPES ====================

export interface LoginFormData {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface RegisterFormData {
  email: string;
  password: string;
  confirmPassword: string;
  firstName: string;
  lastName: string;
  username: string;
  agreeToTerms: boolean;
}

export interface PostFormData {
  title: string;
  content: string;
  emotion: EmotionType;
  topics: string;
  isAnonymous: boolean;
  image?: File;
}

export interface ProfileFormData {
  firstName: string;
  lastName: string;
  username: string;
  bio: string;
  avatar?: File;
}

export interface PreferencesFormData {
  theme: Theme;
  language: string;
  notificationsEnabled: boolean;
  emailNotifications: boolean;
  pushNotifications: boolean;
  privacyLevel: PrivacyLevel;
  aiSuggestionsEnabled: boolean;
  anonymousPostingAllowed: boolean;
}

// ==================== CONSTANTS ====================

export const EMOTIONS: { value: EmotionType; label: string; emoji: string; color: string }[] = [
  { value: 'HAPPY', label: 'Happy', emoji: '😊', color: '#FFD700' },
  { value: 'SAD', label: 'Sad', emoji: '😢', color: '#4682B4' },
  { value: 'ANGRY', label: 'Angry', emoji: '😠', color: '#DC143C' },
  { value: 'ANXIOUS', label: 'Anxious', emoji: '😰', color: '#FF6347' },
  { value: 'EXCITED', label: 'Excited', emoji: '🤩', color: '#FF69B4' },
  { value: 'CALM', label: 'Calm', emoji: '😌', color: '#20B2AA' },
  { value: 'FRUSTRATED', label: 'Frustrated', emoji: '😤', color: '#B22222' },
  { value: 'HOPEFUL', label: 'Hopeful', emoji: '🌟', color: '#32CD32' },
  { value: 'LONELY', label: 'Lonely', emoji: '😔', color: '#696969' },
  { value: 'GRATEFUL', label: 'Grateful', emoji: '🙏', color: '#DAA520' },
  { value: 'CONFUSED', label: 'Confused', emoji: '😕', color: '#9370DB' },
  { value: 'OVERWHELMED', label: 'Overwhelmed', emoji: '😵', color: '#CD853F' },
  { value: 'CONTENT', label: 'Content', emoji: '😊', color: '#90EE90' },
  { value: 'WORRIED', label: 'Worried', emoji: '😟', color: '#F0E68C' },
  { value: 'PROUD', label: 'Proud', emoji: '😌', color: '#FFB6C1' },
  { value: 'DISAPPOINTED', label: 'Disappointed', emoji: '😞', color: '#778899' },
];

export const ENGAGEMENT_TYPES: { value: EngagementType; label: string; icon: string }[] = [
  { value: 'RESONANCE', label: 'Resonates with me', icon: '💫' },
  { value: 'COLLECT', label: 'Save for later', icon: '📌' },
  { value: 'VIEW', label: 'View', icon: '👁️' },
];

export const THEMES: { value: Theme; label: string }[] = [
  { value: 'LIGHT', label: 'Light' },
  { value: 'DARK', label: 'Dark' },
  { value: 'SYSTEM', label: 'System' },
];

export const PRIVACY_LEVELS: { value: PrivacyLevel; label: string; description: string }[] = [
  { value: 'PUBLIC', label: 'Public', description: 'Anyone can see your posts' },
  { value: 'FRIENDS', label: 'Friends', description: 'Only your friends can see your posts' },
  { value: 'PRIVATE', label: 'Private', description: 'Only you can see your posts' },
];

export default {
  EmotionType,
  EngagementType,
  Theme,
  PrivacyLevel,
  EMOTIONS,
  ENGAGEMENT_TYPES,
  THEMES,
  PRIVACY_LEVELS,
};
