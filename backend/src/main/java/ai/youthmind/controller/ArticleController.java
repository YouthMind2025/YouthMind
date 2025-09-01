package ai.youthmind.controller;

import ai.youthmind.dto.ApiResponse;
import ai.youthmind.dto.ArticleCreateRequest;
import ai.youthmind.dto.ArticleDto;
import ai.youthmind.dto.ArticleListResponse;
import ai.youthmind.dto.DocumentParseResponse;
import ai.youthmind.service.ArticleService;
import ai.youthmind.service.DocumentParsingService;
import ai.youthmind.util.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Article Controller - Manages articles for website content
 * 
 * This controller provides endpoints for:
 * - Creating and managing articles (admin functionality)
 * - Retrieving published articles (public/website access)
 * - Article content management and publication workflow
 * 
 * Key Features:
 * - Admin authentication for write operations
 * - Public access for reading published articles
 * - Website-compatible JSON format
 * - Rich content support (HTML)
 * 
 * @author YouthMind Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@RestController
@RequestMapping("/v1/articles")
@Tag(name = "Articles", description = "Article content management operations")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private DocumentParsingService documentParsingService;

    @PostMapping("/create")
    @Operation(
        summary = "Create a new article",
        description = "Create a new article with content and metadata. Requires admin authentication."
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Article created successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required")
    })
    public ResponseEntity<ApiResponse<ArticleDto>> createArticle(@Valid @RequestBody ArticleCreateRequest request) {
        ArticleDto article = articleService.createArticle(request);
        return ResponseBuilder.created(article, "Article created successfully");
    }

    @PostMapping("/upload-docx")
    @Operation(
        summary = "Upload and parse .docx file",
        description = "Upload a .docx file and extract its content for article creation. Requires admin authentication."
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File parsed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file or file type"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required")
    })
    public ResponseEntity<ApiResponse<DocumentParseResponse>> uploadDocxFile(
            @Parameter(description = "The .docx file to upload and parse")
            @RequestParam("file") MultipartFile file
    ) {
        try {
            DocumentParseResponse parseResult = documentParsingService.parseDocxFile(file);
            return ResponseBuilder.success(parseResult, "File parsed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseBuilder.badRequest(e.getMessage());
        } catch (Exception e) {
            return ResponseBuilder.internalServerError("Failed to parse file: " + e.getMessage());
        }
    }

    @GetMapping
    @Operation(
        summary = "Get all published articles",
        description = "Retrieve all published articles for website display. Public access."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Articles retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<ArticleDto>>> getPublishedArticles() {
        List<ArticleDto> articles = articleService.getAllPublishedArticles();
        return ResponseBuilder.success(articles, "Articles retrieved successfully");
    }

    @GetMapping("/admin")
    @Operation(
        summary = "Get articles with pagination (admin)",
        description = "Retrieve articles with pagination for admin panel. Includes unpublished articles."
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Articles retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required")
    })
    public ResponseEntity<ApiResponse<ArticleListResponse>> getArticlesForAdmin(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Number of articles per page", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            
            @Parameter(description = "Filter to only published articles", example = "false")
            @RequestParam(defaultValue = "false") boolean publishedOnly
    ) {
        ArticleListResponse response = articleService.getArticles(page, limit, publishedOnly);
        return ResponseBuilder.success(response, "Articles retrieved successfully");
    }

    @GetMapping("/view/{id}")
    @Operation(
        summary = "Get article by ID",
        description = "Retrieve a specific article by its ID. Returns published articles for public access."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Article retrieved successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Article not found")
    })
    public ResponseEntity<ApiResponse<ArticleDto>> getArticleById(
            @Parameter(description = "Article ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    ) {
        ArticleDto article = articleService.getArticleById(id);
        return ResponseBuilder.success(article, "Article retrieved successfully");
    }

    @PutMapping("/edit/{id}")
    @Operation(
        summary = "Update article",
        description = "Update an existing article. Requires admin authentication."
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Article updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Article not found")
    })
    public ResponseEntity<ApiResponse<ArticleDto>> updateArticle(
            @Parameter(description = "Article ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id,
            @Valid @RequestBody ArticleCreateRequest request
    ) {
        ArticleDto article = articleService.updateArticle(id, request);
        return ResponseBuilder.success(article, "Article updated successfully");
    }

    @DeleteMapping("/delete/{id}")
    @Operation(
        summary = "Delete article",
        description = "Delete an article permanently. Requires admin authentication."
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Article deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Article not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @Parameter(description = "Article ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    ) {
        articleService.deleteArticle(id);
        return ResponseBuilder.success(null, "Article deleted successfully");
    }

    @PostMapping("/manage/{id}/toggle-publish")
    @Operation(
        summary = "Toggle article publication status",
        description = "Toggle the publication status of an article (publish/unpublish). Requires admin authentication."
    )
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Publication status updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Article not found")
    })
    public ResponseEntity<ApiResponse<ArticleDto>> togglePublishStatus(
            @Parameter(description = "Article ID", example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable UUID id
    ) {
        ArticleDto article = articleService.togglePublishStatus(id);
        String message = article.isPublished() ? "Article published successfully" : "Article unpublished successfully";
        return ResponseBuilder.success(article, message);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search articles",
        description = "Search articles by title, content, or introduction. Returns published articles for public access."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    public ResponseEntity<ApiResponse<ArticleListResponse>> searchArticles(
            @Parameter(description = "Search query", example = "AI education")
            @RequestParam String query,
            
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            
            @Parameter(description = "Number of articles per page", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            
            @Parameter(description = "Filter to only published articles", example = "true")
            @RequestParam(defaultValue = "true") boolean publishedOnly
    ) {
        ArticleListResponse response = articleService.searchArticles(query, page, limit, publishedOnly);
        return ResponseBuilder.success(response, "Search completed successfully");
    }
}
