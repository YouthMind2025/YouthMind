package ai.youthmind.service;

import ai.youthmind.dto.ArticleCreateRequest;
import ai.youthmind.dto.ArticleDto;
import ai.youthmind.dto.ArticleListResponse;
import ai.youthmind.dto.PaginationDto;
import ai.youthmind.entity.Article;
import ai.youthmind.exception.ResourceNotFoundException;
import ai.youthmind.repository.ArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ArticleService {
    
    @Autowired
    private ArticleRepository articleRepository;
    
    /**
     * Create a new article
     */
    public ArticleDto createArticle(ArticleCreateRequest request) {
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setIntro(request.getIntro());
        article.setCoverImage(request.getCoverImage());
        article.setPublished(request.getPublished() != null ? request.getPublished() : true);
        
        Article savedArticle = articleRepository.save(article);
        return convertToDto(savedArticle);
    }
    
    /**
     * Get all published articles (for website)
     */
    public List<ArticleDto> getAllPublishedArticles() {
        List<Article> articles = articleRepository.findByPublishedTrueOrderByCreatedAtDesc();
        return articles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get articles with pagination (for admin panel)
     */
    public ArticleListResponse getArticles(int page, int limit, boolean publishedOnly) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Article> articlePage;
        if (publishedOnly) {
            articlePage = articleRepository.findByPublishedTrueOrderByCreatedAtDesc(pageable);
        } else {
            articlePage = articleRepository.findAll(pageable);
        }
        
        List<ArticleDto> articles = articlePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotalPages(articlePage.getTotalPages());
        pagination.setTotalItems((int) articlePage.getTotalElements());
        pagination.setHasNext(!articlePage.isLast());
        pagination.setHasPrevious(!articlePage.isFirst());
        
        ArticleListResponse response = new ArticleListResponse();
        response.setArticles(articles);
        response.setPagination(pagination);
        response.setTotalElements(articlePage.getTotalElements());
        response.setLast(articlePage.isLast());
        
        return response;
    }
    
    /**
     * Get article by ID
     */
    public ArticleDto getArticleById(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        return convertToDto(article);
    }
    
    /**
     * Update article
     */
    public ArticleDto updateArticle(UUID id, ArticleCreateRequest request) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setIntro(request.getIntro());
        article.setCoverImage(request.getCoverImage());
        if (request.getPublished() != null) {
            article.setPublished(request.getPublished());
        }
        
        Article savedArticle = articleRepository.save(article);
        return convertToDto(savedArticle);
    }
    
    /**
     * Delete article
     */
    public void deleteArticle(UUID id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article not found with id: " + id);
        }
        articleRepository.deleteById(id);
    }
    
    /**
     * Toggle article publication status
     */
    public ArticleDto togglePublishStatus(UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + id));
        
        article.setPublished(!article.isPublished());
        Article savedArticle = articleRepository.save(article);
        return convertToDto(savedArticle);
    }
    
    /**
     * Search articles
     */
    public ArticleListResponse searchArticles(String query, int page, int limit, boolean publishedOnly) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Article> articlePage;
        if (publishedOnly) {
            articlePage = articleRepository.searchPublishedArticles(query, pageable);
        } else {
            // For admin panel - search all articles
            articlePage = articleRepository.searchPublishedArticles(query, pageable); // Modify this if needed
        }
        
        List<ArticleDto> articles = articlePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        
        PaginationDto pagination = new PaginationDto();
        pagination.setPage(page);
        pagination.setLimit(limit);
        pagination.setTotalPages(articlePage.getTotalPages());
        pagination.setTotalItems((int) articlePage.getTotalElements());
        pagination.setHasNext(!articlePage.isLast());
        pagination.setHasPrevious(!articlePage.isFirst());
        
        ArticleListResponse response = new ArticleListResponse();
        response.setArticles(articles);
        response.setPagination(pagination);
        response.setTotalElements(articlePage.getTotalElements());
        response.setLast(articlePage.isLast());
        
        return response;
    }
    
    /**
     * Convert Article entity to DTO
     */
    private ArticleDto convertToDto(Article article) {
        ArticleDto dto = new ArticleDto();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setContent(article.getContent());
        dto.setIntro(article.getIntro());
        dto.setCoverImage(article.getCoverImage());
        dto.setPublished(article.isPublished());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        return dto;
    }
}
