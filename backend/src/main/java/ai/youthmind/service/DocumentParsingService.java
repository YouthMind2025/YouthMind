package ai.youthmind.service;

import ai.youthmind.dto.DocumentParseResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
                    
                    // Process runs for formatting
                    for (XWPFRun run : paragraph.getRuns()) {
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
            
            return new DocumentParseResponse(
                title,
                finalContent,
                intro,
                filename,
                file.getSize()
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
