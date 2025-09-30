import React, { useState, useEffect } from 'react';
import { articlesApi } from '../../services/api';
import { Article, ArticleListResponse, ArticleRequest, DocumentParseResponse } from '../../types';
import './Articles.css';

const Articles: React.FC = () => {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [showCreateModal, setShowCreateModal] = useState<boolean>(false);
  const [editingArticle, setEditingArticle] = useState<Article | null>(null);
  const [publishedOnly, setPublishedOnly] = useState<boolean>(false);
  const [uploading, setUploading] = useState<boolean>(false);
  const [uploadSuccess, setUploadSuccess] = useState<string>('');
  const [hasUploadedDocx, setHasUploadedDocx] = useState<boolean>(false);
  const [extractedContent, setExtractedContent] = useState<Partial<DocumentParseResponse>>({});

  // Form state
  const [formData, setFormData] = useState<ArticleRequest>({
    title: '',
    content: '',
    intro: '',
    coverImage: '',
    published: true
  });

  useEffect(() => {
    fetchArticles();
  }, [currentPage, publishedOnly]);

  const fetchArticles = async () => {
    try {
      setLoading(true);
      const response = await articlesApi.getArticles(currentPage, 20, publishedOnly);
      const data: ArticleListResponse = response.data.data;
      
      setArticles(data.articles);
      setTotalPages(data.pagination.totalPages);
      setError('');
    } catch (err) {
      setError('Failed to fetch articles');
      console.error('Error fetching articles:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateArticle = async () => {
    try {
      // Check if we have content either from form or from extracted .docx
      const finalTitle = formData.title.trim() || extractedContent.title || '';
      const finalContent = formData.content.trim() || extractedContent.content || '';
      
      if (!finalTitle || !finalContent) {
        setError('Title and content are required. Please fill them manually or upload a .docx file.');
        return;
      }

      // Create article with final content (form data takes precedence, extracted as fallback)
      const articleData = {
        ...formData,
        title: finalTitle,
        content: finalContent,
        intro: (formData.intro || '').trim() || extractedContent.intro || ''
      };
      
      await articlesApi.createArticle(articleData);
      setShowCreateModal(false);
      resetForm();
      fetchArticles();
    } catch (err) {
      setError('Failed to create article');
      console.error('Error creating article:', err);
    }
  };

  const handleUpdateArticle = async () => {
    try {
      if (!editingArticle || !formData.title.trim() || !formData.content.trim()) {
        setError('Title and content are required');
        return;
      }

      await articlesApi.updateArticle(editingArticle.id, formData);
      setEditingArticle(null);
      resetForm();
      fetchArticles();
    } catch (err) {
      setError('Failed to update article');
      console.error('Error updating article:', err);
    }
  };

  const handleDeleteArticle = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this article?')) {
      return;
    }

    try {
      await articlesApi.deleteArticle(id);
      fetchArticles();
    } catch (err) {
      setError('Failed to delete article');
      console.error('Error deleting article:', err);
    }
  };

  const handleTogglePublish = async (id: string) => {
    try {
      await articlesApi.togglePublishStatus(id);
      fetchArticles();
    } catch (err) {
      setError('Failed to toggle publish status');
      console.error('Error toggling publish status:', err);
    }
  };

  const resetForm = () => {
    setFormData({
      title: '',
      content: '',
      intro: '',
      coverImage: '',
      published: true
    });
    setUploadSuccess('');
    setHasUploadedDocx(false);
    setExtractedContent({});
  };

  const openCreateModal = () => {
    resetForm();
    setEditingArticle(null);
    setShowCreateModal(true);
  };

  const openEditModal = (article: Article) => {
    setFormData({
      title: article.title,
      content: article.content,
      intro: article.intro || '',
      coverImage: article.coverImage || '',
      published: article.published
    });
    setEditingArticle(article);
    setShowCreateModal(true);
  };

  const closeModal = () => {
    setShowCreateModal(false);
    setEditingArticle(null);
    resetForm();
  };

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.name.endsWith('.docx')) {
      setError('Please select a .docx file');
      return;
    }

    setUploading(true);
    setError('');

    try {
      const formDataUpload = new FormData();
      formDataUpload.append('file', file);

      const response = await articlesApi.uploadDocx(formDataUpload);
      const extractedData = response.data.data;

      // Store extracted content separately
      const cleanExtracted = {
        title: extractedData.title && extractedData.title.trim() ? extractedData.title : undefined,
        content: extractedData.content && extractedData.content.trim() ? extractedData.content : undefined,
        intro: extractedData.intro && extractedData.intro.trim() ? extractedData.intro : undefined
      };
      
      setExtractedContent(cleanExtracted);
      setHasUploadedDocx(true);
      
      // Optionally populate form fields for preview (user can still edit)
      setFormData(prev => ({
        ...prev,
        title: cleanExtracted.title || prev.title,
        content: cleanExtracted.content || prev.content,
        intro: cleanExtracted.intro || prev.intro
      }));

      // Show success message and debug info
      if (extractedData.title || extractedData.content) {
        // Clear any previous errors since we got some content
        setError('');
        console.log('Extracted data:', extractedData);
        
        // Show a success message to user
        const extractedItems = [];
        if (extractedData.title) extractedItems.push('title');
        if (extractedData.content) extractedItems.push('content');
        if (extractedData.intro) extractedItems.push('introduction');
        if (extractedData.images && extractedData.images.length > 0) {
          extractedItems.push(`${extractedData.images.length} image${extractedData.images.length > 1 ? 's' : ''}`);
        }
        
        if (extractedItems.length > 0) {
          setUploadSuccess(`Successfully extracted: ${extractedItems.join(', ')}. You can edit the fields below or create the article as-is.`);
          // Clear success message after 8 seconds (longer message)
          setTimeout(() => setUploadSuccess(''), 8000);
        }
      } else {
        setError('No content could be extracted from the file. Please check the file format.');
      }

    } catch (err) {
      setError('Failed to process .docx file. Please try again.');
      console.error('Error uploading file:', err);
    } finally {
      setUploading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  };

  const truncateContent = (content: string, maxLength: number = 100) => {
    const plainText = content.replace(/<[^>]*>/g, '');
    return plainText.length > maxLength ? plainText.substring(0, maxLength) + '...' : plainText;
  };

  return (
    <div className="articles-container">
      <div className="articles-header">
        <h1>Articles Management</h1>
        <div className="articles-actions">
          <label className="filter-checkbox">
            <input
              type="checkbox"
              checked={publishedOnly}
              onChange={(e) => setPublishedOnly(e.target.checked)}
            />
            Published only
          </label>
          <button className="btn-primary" onClick={openCreateModal}>
            Create Article
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}

      {loading ? (
        <div className="loading">Loading articles...</div>
      ) : (
        <>
          <div className="articles-table">
            <table>
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Status</th>
                  <th>Preview</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {articles.map((article) => (
                  <tr key={article.id}>
                    <td className="article-title">
                      <div>
                        <strong>{article.title}</strong>
                        {article.intro && (
                          <div className="article-intro">{article.intro}</div>
                        )}
                      </div>
                    </td>
                    <td>
                      <span className={`status-badge ${article.published ? 'published' : 'draft'}`}>
                        {article.published ? 'Published' : 'Draft'}
                      </span>
                    </td>
                    <td className="article-preview">
                      {truncateContent(article.content)}
                    </td>
                    <td>{formatDate(article.createdAt)}</td>
                    <td className="article-actions">
                      <button
                        className="btn-secondary btn-sm"
                        onClick={() => openEditModal(article)}
                      >
                        Edit
                      </button>
                      <button
                        className={`btn-sm ${article.published ? 'btn-warning' : 'btn-success'}`}
                        onClick={() => handleTogglePublish(article.id)}
                      >
                        {article.published ? 'Unpublish' : 'Publish'}
                      </button>
                      <button
                        className="btn-danger btn-sm"
                        onClick={() => handleDeleteArticle(article.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {totalPages > 1 && (
            <div className="pagination">
              <button
                disabled={currentPage === 0}
                onClick={() => setCurrentPage(currentPage - 1)}
              >
                Previous
              </button>
              <span>Page {currentPage + 1} of {totalPages}</span>
              <button
                disabled={currentPage >= totalPages - 1}
                onClick={() => setCurrentPage(currentPage + 1)}
              >
                Next
              </button>
            </div>
          )}
        </>
      )}

      {/* Create/Edit Modal */}
      {showCreateModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editingArticle ? 'Edit Article' : 'Create New Article'}</h2>
              <button className="close-btn" onClick={closeModal}>×</button>
            </div>
            
            <div className="modal-body">
              <div className="form-group">
                <label>Title{hasUploadedDocx && extractedContent.title ? ' (optional - extracted from file)' : '*'}</label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({...formData, title: e.target.value})}
                  placeholder="Enter article title"
                />
              </div>

              <div className="form-group">
                <label>Introduction</label>
                <textarea
                  value={formData.intro}
                  onChange={(e) => setFormData({...formData, intro: e.target.value})}
                  placeholder="Brief introduction or summary"
                  rows={3}
                />
              </div>

              <div className="form-group">
                <label>Cover Image URL</label>
                <input
                  type="text"
                  value={formData.coverImage}
                  onChange={(e) => setFormData({...formData, coverImage: e.target.value})}
                  placeholder="https://example.com/image.jpg"
                />
              </div>

              <div className="form-group">
                <label>Upload .docx File (Optional)</label>
                <input
                  type="file"
                  accept=".docx"
                  onChange={handleFileUpload}
                  className="file-input"
                  disabled={uploading}
                />
                <small className="file-help">
                  {uploading ? 'Processing file...' : 'Upload a .docx file to automatically extract content'}
                </small>
                {uploadSuccess && (
                  <div className="upload-success">
                    ✅ {uploadSuccess}
                  </div>
                )}
              </div>

              <div className="form-group">
                <label>Content{hasUploadedDocx && extractedContent.content ? ' (optional - extracted from file)' : '*'}</label>
                <textarea
                  value={formData.content}
                  onChange={(e) => setFormData({...formData, content: e.target.value})}
                  placeholder="Article content (HTML supported)"
                  rows={10}
                />
              </div>

              <div className="form-group">
                <label className="checkbox-label">
                  <input
                    type="checkbox"
                    checked={formData.published}
                    onChange={(e) => setFormData({...formData, published: e.target.checked})}
                  />
                  Publish immediately
                </label>
              </div>
            </div>

            <div className="modal-footer">
              <button className="btn-secondary" onClick={closeModal}>
                Cancel
              </button>
              <button 
                className="btn-primary" 
                onClick={editingArticle ? handleUpdateArticle : handleCreateArticle}
              >
                {editingArticle ? 'Update Article' : 'Create Article'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Articles;
