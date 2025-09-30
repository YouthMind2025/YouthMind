package ai.youthmind.service;

import ai.youthmind.dto.DocumentParseResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

@Service
public class DocumentParsingService {
    
    /**
     * Parse a .docx file and extract content
     */
    public DocumentParseResponse parseDocxFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".docx")) {
            throw new IllegalArgumentException("Only .docx files are supported");
        }
        
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder htmlContent = new StringBuilder();
            String title = "";
            String intro = "";
            
            // Extract images from document
            List<String> images = new ArrayList<>();
            Map<String, Map<String, Object>> imageMetadata = new HashMap<>();
            extractImages(document, images, imageMetadata);
            
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            
            boolean firstParagraphProcessed = false;
            boolean titleExtracted = false;
            
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText().trim();
                
                if (text.isEmpty()) {
                    continue;
                }
                
                // Extract title from first non-empty paragraph or if it's a heading
                if (!titleExtracted && (!firstParagraphProcessed || isHeading(paragraph))) {
                    title = text;
                    titleExtracted = true;
                    firstParagraphProcessed = true;
                    // Don't add title to content - it will be displayed separately
                    continue;
                }
                
                // Extract intro from first paragraph after title
                if (intro.isEmpty() && firstParagraphProcessed) {
                    intro = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                }
                
                // Convert paragraph to HTML
                if (isHeading(paragraph)) {
                    htmlContent.append("<h2>").append(escapeHtml(text)).append("</h2>\n");
                } else {
                    htmlContent.append("<p>");
                    
                    // Process runs for formatting and inline images
                    for (XWPFRun run : paragraph.getRuns()) {
                        // Check for embedded pictures and embed them
                        List<XWPFPicture> pictures = run.getEmbeddedPictures();
                        if (!pictures.isEmpty()) {
                            System.out.println("Embedding " + pictures.size() + " pictures in HTML");
                            for (XWPFPicture picture : pictures) {
                                // Find matching image in our extracted list
                                String imageHtml = findAndEmbedImage(picture, images);
                                if (imageHtml != null) {
                                    htmlContent.append(imageHtml);
                                    System.out.println("Embedded image in HTML");
                                }
                            }
                        }
                        
                        // Process text content
                        String runText = run.getText(0);
                        if (runText != null) {
                            String formattedText = escapeHtml(runText);
                            
                            if (run.isBold()) {
                                formattedText = "<strong>" + formattedText + "</strong>";
                            }
                            if (run.isItalic()) {
                                formattedText = "<em>" + formattedText + "</em>";
                            }
                            
                            htmlContent.append(formattedText);
                        }
                    }
                    
                    htmlContent.append("</p>\n");
                }
                
                firstParagraphProcessed = true;
            }
            
            // If no title was extracted, use filename
            if (title.isEmpty() && filename != null) {
                title = filename.replace(".docx", "").replace(".DOCX", "");
            }
            
            // If still no intro, try to extract from content
            if (intro.isEmpty() && htmlContent.length() > 0) {
                String plainText = htmlContent.toString().replaceAll("<[^>]*>", "").trim();
                if (plainText.length() > 0) {
                    intro = plainText.length() > 200 ? plainText.substring(0, 200) + "..." : plainText;
                }
            }
            
            // Ensure we have some content
            String finalContent = htmlContent.toString().trim();
            if (finalContent.isEmpty() && !paragraphs.isEmpty()) {
                // Fallback: just extract all text as paragraphs
                StringBuilder fallbackContent = new StringBuilder();
                for (XWPFParagraph paragraph : paragraphs) {
                    String text = paragraph.getText().trim();
                    if (!text.isEmpty()) {
                        fallbackContent.append("<p>").append(escapeHtml(text)).append("</p>\n");
                    }
                }
                finalContent = fallbackContent.toString();
            }
            
            // If we have images but they weren't embedded inline, add them at the end
            if (!images.isEmpty() && !finalContent.contains("<img")) {
                System.out.println("Images not embedded inline, adding at the end of content");
                StringBuilder imageSection = new StringBuilder();
                imageSection.append("<div class=\"document-images\">\n");
                for (int i = 0; i < images.size(); i++) {
                    String imageUrl = images.get(i);
                    imageSection.append("<img src=\"").append(imageUrl)
                              .append("\" alt=\"Image ").append(i + 1)
                              .append("\" style=\"max-width: 100%; height: auto; display: block; margin: 10px 0;\" />\n");
                }
                imageSection.append("</div>\n");
                finalContent += imageSection.toString();
                System.out.println("Added " + images.size() + " images at the end of content");
            }
            
            return new DocumentParseResponse(
                title,
                finalContent,
                intro,
                filename,
                file.getSize(),
                images,
                imageMetadata
            );
        }
    }
    
    /**
     * Check if paragraph is a heading based on style or formatting
     */
    private boolean isHeading(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        return style != null && (style.contains("Heading") || style.contains("Title"));
    }
    
    /**
     * Extract images from document using multiple methods
     */
    private void extractImages(XWPFDocument document, List<String> images, Map<String, Map<String, Object>> imageMetadata) {
        System.out.println("Starting comprehensive image extraction...");
        
        try {
            // Method 1: Extract from document relations (most comprehensive)
            extractImagesFromRelations(document, images, imageMetadata);
            
            // Method 2: Extract from paragraphs and runs (fallback)
            extractImagesFromParagraphs(document, images, imageMetadata);
            
            System.out.println("Image extraction completed. Found " + images.size() + " images.");
        } catch (Exception e) {
            System.err.println("Error during comprehensive image extraction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Extract images from document relations (most comprehensive method)
     */
    private void extractImagesFromRelations(XWPFDocument document, List<String> images, Map<String, Map<String, Object>> imageMetadata) {
        try {
            // Get all picture data from the document
            List<XWPFPictureData> pictureDataList = document.getAllPictures();
            System.out.println("Found " + pictureDataList.size() + " pictures in document relations");
            
            for (XWPFPictureData pictureData : pictureDataList) {
                try {
                    // Get image data
                    byte[] imageData = pictureData.getData();
                    
                    // Determine image type
                    int pictureType = pictureData.getPictureType();
                    String imageType = getImageTypeFromPictureType(pictureType);
                    String mimeType = "image/" + imageType;
                    
                    // Convert to base64 data URL
                    String base64Image = Base64.getEncoder().encodeToString(imageData);
                    String dataUrl = "data:" + mimeType + ";base64," + base64Image;
                    
                    // Add to images list
                    images.add(dataUrl);
                    
                    // Add metadata
                    String imageId = "image_" + images.size();
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("size", imageData.length);
                    metadata.put("type", imageType);
                    metadata.put("mimeType", mimeType);
                    metadata.put("filename", pictureData.getFileName() != null ? pictureData.getFileName() : imageId + "." + imageType);
                    
                    imageMetadata.put(imageId, metadata);
                    
                    System.out.println("Extracted image: " + imageId + " (" + imageData.length + " bytes, " + imageType + ")");
                    
                } catch (Exception e) {
                    System.err.println("Error processing picture data: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting images from relations: " + e.getMessage());
        }
    }
    
    /**
     * Extract images from paragraphs and runs (fallback method)
     */
    private void extractImagesFromParagraphs(XWPFDocument document, List<String> images, Map<String, Map<String, Object>> imageMetadata) {
        try {
            int initialImageCount = images.size();
            
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                for (XWPFRun run : paragraph.getRuns()) {
                    List<XWPFPicture> pictures = run.getEmbeddedPictures();
                    if (!pictures.isEmpty()) {
                        System.out.println("Found " + pictures.size() + " embedded pictures in paragraph run");
                        for (XWPFPicture picture : pictures) {
                            // Only add if not already extracted from relations
                            if (!isImageAlreadyExtracted(picture, images)) {
                                processImageFromRun(picture, images, imageMetadata);
                            }
                        }
                    }
                }
            }
            
            int newImagesFromParagraphs = images.size() - initialImageCount;
            System.out.println("Extracted " + newImagesFromParagraphs + " additional images from paragraphs");
            
        } catch (Exception e) {
            System.err.println("Error extracting images from paragraphs: " + e.getMessage());
        }
    }
    
    /**
     * Check if image is already extracted to avoid duplicates
     */
    private boolean isImageAlreadyExtracted(XWPFPicture picture, List<String> images) {
        try {
            byte[] imageData = picture.getPictureData().getData();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            
            for (String existingImage : images) {
                if (existingImage.contains(base64Image)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // If we can't check, assume it's not extracted
        }
        return false;
    }
    
    /**
     * Process image from paragraph run
     */
    private void processImageFromRun(XWPFPicture picture, List<String> images, Map<String, Map<String, Object>> imageMetadata) {
        try {
            byte[] imageData = picture.getPictureData().getData();
            int pictureType = picture.getPictureData().getPictureType();
            String imageType = getImageTypeFromPictureType(pictureType);
            String mimeType = "image/" + imageType;
            
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            String dataUrl = "data:" + mimeType + ";base64," + base64Image;
            
            images.add(dataUrl);
            
            String imageId = "image_" + images.size();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("size", imageData.length);
            metadata.put("type", imageType);
            metadata.put("mimeType", mimeType);
            metadata.put("filename", picture.getDescription() != null ? picture.getDescription() : imageId + "." + imageType);
            
            imageMetadata.put(imageId, metadata);
            
            System.out.println("Extracted image from run: " + imageId);
        } catch (Exception e) {
            System.err.println("Error processing image from run: " + e.getMessage());
        }
    }
    
    /**
     * Find matching image and return HTML img tag
     */
    private String findAndEmbedImage(XWPFPicture picture, List<String> images) {
        try {
            byte[] imageData = picture.getPictureData().getData();
            String base64Image = Base64.getEncoder().encodeToString(imageData);
            
            // Find matching image in our extracted list
            for (String dataUrl : images) {
                if (dataUrl.contains(base64Image)) {
                    String altText = picture.getDescription() != null ? picture.getDescription() : "Image";
                    return "<img src=\"" + dataUrl + "\" alt=\"" + escapeHtml(altText) + 
                           "\" style=\"max-width: 100%; height: auto; display: block; margin: 10px 0;\" />";
                }
            }
        } catch (Exception e) {
            System.err.println("Error finding and embedding image: " + e.getMessage());
        }
        return null;
    }

    
    /**
     * Get image file extension from picture type
     * Using Apache POI picture type constants values
     */
    private String getImageTypeFromPictureType(int pictureType) {
        switch (pictureType) {
            case 5: // PICTURE_TYPE_JPEG
                return "jpg";
            case 6: // PICTURE_TYPE_PNG
                return "png";
            case 7: // PICTURE_TYPE_GIF
                return "gif";
            case 2: // PICTURE_TYPE_BMP
                return "bmp";
            case 8: // PICTURE_TYPE_TIFF
                return "tiff";
            case 3: // PICTURE_TYPE_WMF
                return "wmf";
            case 4: // PICTURE_TYPE_EMF
                return "emf";
            default:
                return "png"; // Default fallback
        }
    }
    

    
    /**
     * Check if paragraph contains images
     */
    private boolean hasImages(XWPFParagraph paragraph) {
        for (XWPFRun run : paragraph.getRuns()) {
            if (!run.getEmbeddedPictures().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
