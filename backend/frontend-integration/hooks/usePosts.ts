// React Hooks for Posts Management in YouthMind Frontend
import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { PostsAPI } from '../api/posts';
import { useAuthStore } from '../stores/authStore';
import { useNotificationStore } from '../stores/notificationStore';
import type {
  Post,
  CreatePostRequest,
  UpdatePostRequest,
  EmotionType,
  EngagementType,
  FeedParams,
  SearchParams,
  TrendingParams,
} from '../api/types';

// ==================== QUERY KEYS ====================
export const postQueryKeys = {
  all: ['posts'] as const,
  feeds: () => [...postQueryKeys.all, 'feed'] as const,
  feed: (params: FeedParams) => [...postQueryKeys.feeds(), params] as const,
  trending: (params: TrendingParams) => [...postQueryKeys.all, 'trending', params] as const,
  search: (params: SearchParams) => [...postQueryKeys.all, 'search', params] as const,
  post: (id: string) => [...postQueryKeys.all, 'post', id] as const,
  userPosts: (userId: string) => [...postQueryKeys.all, 'user', userId] as const,
  analytics: (postId: string) => [...postQueryKeys.all, 'analytics', postId] as const,
};

// ==================== FEED HOOKS ====================

/**
 * Hook for infinite scrolling feed with emotion filtering
 */
export const useFeed = (params: FeedParams = {}) => {
  const queryClient = useQueryClient();

  return useInfiniteQuery({
    queryKey: postQueryKeys.feed(params),
    queryFn: ({ pageParam = 0 }) =>
      PostsAPI.getFeed({ ...params, page: pageParam }),
    getNextPageParam: (lastPage) => 
      lastPage.hasNext ? lastPage.currentPage + 1 : undefined,
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 10 * 60 * 1000, // 10 minutes
    refetchOnWindowFocus: false,
    refetchOnMount: true,
  });
};

/**
 * Hook for trending posts
 */
export const useTrendingPosts = (params: TrendingParams = {}) => {
  return useInfiniteQuery({
    queryKey: postQueryKeys.trending(params),
    queryFn: ({ pageParam = 0 }) =>
      PostsAPI.getTrendingPosts({ ...params, page: pageParam }),
    getNextPageParam: (lastPage) => 
      lastPage.posts.length === (params.size || 20) ? (lastPage.posts.length / (params.size || 20)) : undefined,
    staleTime: 2 * 60 * 1000, // 2 minutes for trending content
    cacheTime: 5 * 60 * 1000, // 5 minutes
  });
};

/**
 * Hook for searching posts
 */
export const useSearchPosts = (params: SearchParams) => {
  return useInfiniteQuery({
    queryKey: postQueryKeys.search(params),
    queryFn: ({ pageParam = 0 }) =>
      PostsAPI.searchPosts({ ...params, page: pageParam }),
    getNextPageParam: (lastPage, allPages) => 
      lastPage.posts.length === (params.size || 20) ? allPages.length : undefined,
    enabled: !!params.query && params.query.length > 2,
    staleTime: 30 * 1000, // 30 seconds for search results
    cacheTime: 2 * 60 * 1000, // 2 minutes
  });
};

// ==================== SINGLE POST HOOKS ====================

/**
 * Hook for fetching a single post
 */
export const usePost = (postId: string) => {
  return useQuery({
    queryKey: postQueryKeys.post(postId),
    queryFn: () => PostsAPI.getPost(postId),
    enabled: !!postId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 10 * 60 * 1000, // 10 minutes
  });
};

/**
 * Hook for post analytics
 */
export const usePostAnalytics = (postId: string) => {
  return useQuery({
    queryKey: postQueryKeys.analytics(postId),
    queryFn: () => PostsAPI.getPostAnalytics(postId),
    enabled: !!postId,
    staleTime: 60 * 1000, // 1 minute
    cacheTime: 5 * 60 * 1000, // 5 minutes
  });
};

// ==================== USER POSTS HOOKS ====================

/**
 * Hook for user's posts
 */
export const useUserPosts = (userId: string, params: { page?: number; size?: number } = {}) => {
  return useInfiniteQuery({
    queryKey: [...postQueryKeys.userPosts(userId), params],
    queryFn: ({ pageParam = 0 }) =>
      PostsAPI.getUserPosts(userId, { ...params, page: pageParam }),
    getNextPageParam: (lastPage) => 
      lastPage.hasNext ? lastPage.currentPage + 1 : undefined,
    enabled: !!userId,
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 10 * 60 * 1000, // 10 minutes
  });
};

/**
 * Hook for user's liked posts
 */
export const useUserLikedPosts = (userId: string) => {
  return useInfiniteQuery({
    queryKey: [...postQueryKeys.userPosts(userId), 'liked'],
    queryFn: ({ pageParam = 0 }) =>
      PostsAPI.getUserLikedPosts(userId, { page: pageParam }),
    getNextPageParam: (lastPage) => 
      lastPage.hasNext ? lastPage.currentPage + 1 : undefined,
    enabled: !!userId,
    staleTime: 5 * 60 * 1000,
    cacheTime: 10 * 60 * 1000,
  });
};

/**
 * Hook for user's bookmarked posts
 */
export const useUserBookmarkedPosts = (userId: string) => {
  return useInfiniteQuery({
    queryKey: [...postQueryKeys.userPosts(userId), 'bookmarked'],
    queryFn: ({ pageParam = 0 }) =>
      PostsAPI.getUserBookmarkedPosts(userId, { page: pageParam }),
    getNextPageParam: (lastPage) => 
      lastPage.hasNext ? lastPage.currentPage + 1 : undefined,
    enabled: !!userId,
    staleTime: 5 * 60 * 1000,
    cacheTime: 10 * 60 * 1000,
  });
};

// ==================== MUTATION HOOKS ====================

/**
 * Hook for creating posts
 */
export const useCreatePost = () => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { showSuccess, showError } = useNotificationStore();

  return useMutation({
    mutationFn: (postData: CreatePostRequest) => PostsAPI.createPost(postData),
    onSuccess: (newPost) => {
      // Invalidate and refetch feed queries
      queryClient.invalidateQueries({ queryKey: postQueryKeys.feeds() });
      
      // Add to user's posts cache
      if (user?.id) {
        queryClient.invalidateQueries({ 
          queryKey: postQueryKeys.userPosts(user.id) 
        });
      }

      // Show success notification
      showSuccess('Post created successfully!');
      
      // Optimistically update feed if possible
      queryClient.setQueryData(
        postQueryKeys.feed({}),
        (oldData: any) => {
          if (!oldData) return oldData;
          
          return {
            ...oldData,
            pages: [
              {
                ...oldData.pages[0],
                posts: [newPost, ...oldData.pages[0].posts],
              },
              ...oldData.pages.slice(1),
            ],
          };
        }
      );
    },
    onError: (error: any) => {
      showError(error.message || 'Failed to create post');
    },
  });
};

/**
 * Hook for updating posts
 */
export const useUpdatePost = () => {
  const queryClient = useQueryClient();
  const { showSuccess, showError } = useNotificationStore();

  return useMutation({
    mutationFn: ({ postId, postData }: { postId: string; postData: UpdatePostRequest }) =>
      PostsAPI.updatePost(postId, postData),
    onSuccess: (updatedPost) => {
      // Update specific post cache
      queryClient.setQueryData(
        postQueryKeys.post(updatedPost.id),
        updatedPost
      );

      // Invalidate related queries
      queryClient.invalidateQueries({ queryKey: postQueryKeys.feeds() });
      queryClient.invalidateQueries({ 
        queryKey: postQueryKeys.userPosts(updatedPost.userId) 
      });

      showSuccess('Post updated successfully!');
    },
    onError: (error: any) => {
      showError(error.message || 'Failed to update post');
    },
  });
};

/**
 * Hook for deleting posts
 */
export const useDeletePost = () => {
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { showSuccess, showError } = useNotificationStore();

  return useMutation({
    mutationFn: (postId: string) => PostsAPI.deletePost(postId),
    onSuccess: (_, postId) => {
      // Remove from all caches
      queryClient.removeQueries({ queryKey: postQueryKeys.post(postId) });
      queryClient.invalidateQueries({ queryKey: postQueryKeys.feeds() });
      
      if (user?.id) {
        queryClient.invalidateQueries({ 
          queryKey: postQueryKeys.userPosts(user.id) 
        });
      }

      showSuccess('Post deleted successfully!');
    },
    onError: (error: any) => {
      showError(error.message || 'Failed to delete post');
    },
  });
};

// ==================== ENGAGEMENT HOOKS ====================

/**
 * Hook for post engagement actions
 */
export const usePostEngagement = () => {
  const queryClient = useQueryClient();
  const { showError } = useNotificationStore();

  const engageMutation = useMutation({
    mutationFn: ({ 
      postId, 
      type, 
      currentlyEngaged 
    }: { 
      postId: string; 
      type: EngagementType; 
      currentlyEngaged: boolean;
    }) => PostsAPI.toggleEngagement(postId, type, currentlyEngaged),
    
    onMutate: async ({ postId, type, currentlyEngaged }) => {
      // Cancel any outgoing refetches
      await queryClient.cancelQueries({ queryKey: postQueryKeys.post(postId) });

      // Snapshot the previous value
      const previousPost = queryClient.getQueryData(postQueryKeys.post(postId));

      // Optimistically update the post
      queryClient.setQueryData(postQueryKeys.post(postId), (old: Post | undefined) => {
        if (!old) return old;

        const delta = currentlyEngaged ? -1 : 1;
        const updates: Partial<Post> = {};

        switch (type) {
          case 'RESONANCE':
            updates.resonanceCount = Math.max(0, old.resonanceCount + delta);
            updates.isResonated = !currentlyEngaged;
            break;
          case 'COLLECT':
            updates.collectCount = Math.max(0, old.collectCount + delta);
            updates.isCollected = !currentlyEngaged;
            break;
          case 'VIEW':
            updates.views = Math.max(0, old.views + delta);
            break;
        }

        return { ...old, ...updates };
      });

      return { previousPost, postId };
    },

    onError: (error: any, variables, context) => {
      // Revert the optimistic update
      if (context?.previousPost) {
        queryClient.setQueryData(
          postQueryKeys.post(context.postId),
          context.previousPost
        );
      }
      showError(error.message || 'Failed to update engagement');
    },

    onSettled: (_, __, { postId }) => {
      // Always refetch after error or success
      queryClient.invalidateQueries({ queryKey: postQueryKeys.post(postId) });
    },
  });

  const engage = useCallback(
    (postId: string, type: EngagementType, currentlyEngaged: boolean = false) => {
      return engageMutation.mutate({ postId, type, currentlyEngaged });
    },
    [engageMutation]
  );

  return {
    engage,
    isLoading: engageMutation.isLoading,
    error: engageMutation.error,
  };
};

/**
 * Hook for incrementing post views
 */
export const useIncrementViews = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (postId: string) => PostsAPI.incrementViews(postId),
    onSuccess: (_, postId) => {
      // Silently update view count in cache
      queryClient.setQueryData(postQueryKeys.post(postId), (old: Post | undefined) => {
        if (!old) return old;
        return { ...old, views: old.views + 1 };
      });
    },
    // Don't show errors for view tracking
    onError: () => {},
  });
};

// ==================== COMPOSED HOOKS ====================

/**
 * Comprehensive posts hook that combines feed, creation, and engagement
 */
export const usePosts = (params: FeedParams = {}) => {
  const feedQuery = useFeed(params);
  const createMutation = useCreatePost();
  const updateMutation = useUpdatePost();
  const deleteMutation = useDeletePost();
  const { engage } = usePostEngagement();
  const incrementViews = useIncrementViews();

  // Flatten all posts from all pages
  const posts = useMemo(() => {
    return feedQuery.data?.pages.flatMap(page => page.posts) ?? [];
  }, [feedQuery.data]);

  return {
    // Feed data
    posts,
    isLoading: feedQuery.isLoading,
    error: feedQuery.error,
    hasNextPage: feedQuery.hasNextPage,
    isFetchingNextPage: feedQuery.isFetchingNextPage,
    
    // Feed actions
    loadMore: feedQuery.fetchNextPage,
    refresh: feedQuery.refetch,
    
    // Post mutations
    createPost: createMutation.mutate,
    updatePost: (postId: string, data: UpdatePostRequest) => 
      updateMutation.mutate({ postId, postData: data }),
    deletePost: deleteMutation.mutate,
    
    // Engagement actions
    engagePost: engage,
    incrementViews: incrementViews.mutate,
    
    // Loading states
    isCreating: createMutation.isLoading,
    isUpdating: updateMutation.isLoading,
    isDeleting: deleteMutation.isLoading,
  };
};

/**
 * Hook for emotion-filtered posts
 */
export const useEmotionPosts = (emotion: EmotionType) => {
  return usePosts({ emotion });
};

/**
 * Hook for user's own posts with management capabilities
 */
export const useMyPosts = () => {
  const { user } = useAuthStore();
  
  return usePosts({ userId: user?.id });
};

export default {
  useFeed,
  useTrendingPosts,
  useSearchPosts,
  usePost,
  usePostAnalytics,
  useUserPosts,
  useUserLikedPosts,
  useUserBookmarkedPosts,
  useCreatePost,
  useUpdatePost,
  useDeletePost,
  usePostEngagement,
  useIncrementViews,
  usePosts,
  useEmotionPosts,
  useMyPosts,
};
